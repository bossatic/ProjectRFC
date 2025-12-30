/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.jms.JMSException
 *  javax.jms.Message
 *  javax.jms.Queue
 *  javax.jms.QueueConnection
 *  javax.jms.QueueSender
 *  javax.jms.QueueSession
 *  javax.jms.Topic
 *  javax.jms.TopicConnection
 *  javax.jms.TopicPublisher
 *  javax.jms.TopicSession
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.InitializingBean
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Qualifier
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.core.task.TaskExecutor
 *  org.springframework.stereotype.Component
 *  org.talend.sap.idoc.ISAPIDoc
 *  org.talend.sap.idoc.ISAPIDocPackage
 *  org.talend.sap.impl.idoc.SAPIDocUtil
 *  org.talend.sap.impl.server.configuration.Holder
 */
package org.talend.sap.impl.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.talend.sap.idoc.ISAPIDoc;
import org.talend.sap.idoc.ISAPIDocPackage;
import org.talend.sap.impl.idoc.SAPIDocUtil;
import org.talend.sap.impl.server.SAPIDocNameUtil;
import org.talend.sap.impl.server.configuration.Holder;

@Component
public class SAPIDocPublisher
implements Runnable,
InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SAPIDocPublisher.class);
    @Value(value="${jms.durable.queue.replicate:false}")
    protected boolean replicate;
    @Value(value="${jms.durable.queue.retentionPeriod:604800000}")
    protected long retentionPeriod;
    @Autowired
    protected TaskExecutor taskExecutor;
    @Autowired
    @Qualifier(value="idocQueue")
    protected BlockingQueue<ISAPIDocPackage> idocQueue;
    @Autowired(required=false)
    protected Holder<TopicConnection> topicConnection;
    @Autowired(required=false)
    protected Holder<QueueConnection> queueConnection;
    private final Map<String, TopicPublisher> topicPublisherMap = new HashMap<String, TopicPublisher>();
    private final Map<String, QueueSender> queueSenderMap = new HashMap<String, QueueSender>();
    private boolean jmsBrokerConfigured;

    public void afterPropertiesSet() throws Exception {
        this.jmsBrokerConfigured = this.topicConnection != null;
        this.taskExecutor.execute((Runnable)this);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public void run() {
        ISAPIDocPackage idocPackage = null;
        while (true) {
            try {
                idocPackage = this.idocQueue.take();
            }
            catch (InterruptedException e) {
                return;
            }
            if (idocPackage == null || SAPIDocUtil.isPoison((ISAPIDocPackage)idocPackage)) return;
            if (!this.jmsBrokerConfigured) {
                LOGGER.error("No JMS broker (embedded or remote) configured");
                idocPackage.rollback("No JMS broker (embedded or remote) configured");
                continue;
            }
            LOGGER.info("{}: Publishing IDOC package '{}' with size: {}", new Object[]{idocPackage.getPartnerHost(), idocPackage.getTID(), idocPackage.size()});
            try {
                this.publishToTopic(idocPackage);
                if (this.replicate) {
                    this.sendToQueue(idocPackage);
                }
                idocPackage.commit();
                continue;
            }
            catch (JMSException e) {
                LOGGER.error("{}: Rolling back SAP transaction '{}': {}", new Object[]{idocPackage.getPartnerHost(), idocPackage.getTID(), e.getMessage()});
                idocPackage.rollback(e.getMessage());
                continue;
            }
            catch (RuntimeException e) {
                String message = "{}: Unexpected error while publishing IDOC package within transaction '{}': {}";
                LOGGER.error(message, new Object[]{idocPackage.getPartnerHost(), idocPackage.getTID(), e.getMessage()});
                throw new RuntimeException("Unexpected error while publishing IDOC package", e);
            }
            break;
        }
    }

    protected void publishToTopic(ISAPIDocPackage idocPackage) throws JMSException {
        TopicSession session = null;
        try {
            session = ((TopicConnection)this.topicConnection.get()).createTopicSession(false, 1);
            for (ISAPIDoc idoc : idocPackage) {
                this.publishToTopic(idoc, session);
            }
        }
        catch (JMSException e) {
            LOGGER.error("{}: Error while publishing IDOC package within transaction {}", (Object)idocPackage.getPartnerHost(), (Object)idocPackage.getTID());
            throw e;
        }
        finally {
            for (Map.Entry<String, TopicPublisher> entry : this.topicPublisherMap.entrySet()) {
                try {
                    entry.getValue().close();
                }
                catch (JMSException e) {
                    LOGGER.error("Error while closing publisher for topic {}", (Object)entry.getKey());
                }
            }
            this.topicPublisherMap.clear();
            if (session != null) {
                try {
                    session.close();
                }
                catch (JMSException e) {
                    LOGGER.error("{}: Error while closing topic session for IDOC package in transaction {}", (Object)idocPackage.getPartnerHost(), (Object)idocPackage.getTID());
                }
            }
        }
    }

    protected void publishToTopic(ISAPIDoc idoc, TopicSession session) throws JMSException {
        String topicName = SAPIDocNameUtil.getTopicName(idoc);
        try {
            TopicPublisher publisher = this.getTopicPublisher(session, topicName);
            publisher.publish((Message)session.createTextMessage(idoc.toString()));
        }
        catch (JMSException e) {
            LOGGER.error("Error while publishing IDOC {} to topic {}", (Object)SAPIDocNameUtil.getIDocFriendlyName(idoc), (Object)topicName);
            throw e;
        }
    }

    protected void sendToQueue(ISAPIDocPackage idocPackage) throws JMSException {
        QueueSession session = null;
        try {
            session = ((QueueConnection)this.queueConnection.get()).createQueueSession(false, 1);
            for (ISAPIDoc idoc : idocPackage) {
                this.sendToQueue(idoc, session);
            }
        }
        catch (JMSException e) {
            LOGGER.error("{}: Error while sending IDOC package within transaction {}", (Object)idocPackage.getPartnerHost(), (Object)idocPackage.getTID());
            throw e;
        }
        finally {
            for (Map.Entry<String, QueueSender> entry : this.queueSenderMap.entrySet()) {
                try {
                    entry.getValue().close();
                }
                catch (JMSException e) {
                    LOGGER.error("Error while closing sender for queue {}", (Object)entry.getKey());
                }
            }
            this.queueSenderMap.clear();
            if (session != null) {
                try {
                    session.close();
                }
                catch (JMSException e) {
                    LOGGER.error("{}: Error while closing queue session for IDOC package in transaction {}", (Object)idocPackage.getPartnerHost(), (Object)idocPackage.getTID());
                }
            }
        }
    }

    protected void sendToQueue(ISAPIDoc idoc, QueueSession session) throws JMSException {
        String queueName = SAPIDocNameUtil.getQueueName(idoc);
        try {
            QueueSender sender = this.getQueueSender(session, queueName);
            sender.send((Message)session.createTextMessage(idoc.toString()));
        }
        catch (JMSException e) {
            LOGGER.error("Error while sending IDOC {} to queue {}", (Object)SAPIDocNameUtil.getIDocFriendlyName(idoc), (Object)queueName);
            throw e;
        }
    }

    protected QueueSender getQueueSender(QueueSession session, String queueName) throws JMSException {
        QueueSender sender = this.queueSenderMap.get(queueName);
        if (sender == null) {
            Queue queue = session.createQueue(queueName);
            sender = session.createSender(queue);
            sender.setDeliveryMode(2);
            sender.setTimeToLive(this.retentionPeriod);
            this.queueSenderMap.put(queueName, sender);
        }
        return sender;
    }

    protected TopicPublisher getTopicPublisher(TopicSession session, String topicName) throws JMSException {
        TopicPublisher publisher = this.topicPublisherMap.get(topicName);
        if (publisher == null) {
            Topic topic = session.createTopic(topicName);
            publisher = session.createPublisher(topic);
            this.topicPublisherMap.put(topicName, publisher);
        }
        return publisher;
    }
}
