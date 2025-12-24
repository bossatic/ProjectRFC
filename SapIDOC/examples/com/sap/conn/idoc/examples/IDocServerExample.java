package com.sap.conn.idoc.examples;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.sap.conn.idoc.IDocDocumentList;
import com.sap.conn.idoc.IDocXMLProcessor;
import com.sap.conn.idoc.jco.JCoIDoc;
import com.sap.conn.idoc.jco.JCoIDocHandler;
import com.sap.conn.idoc.jco.JCoIDocHandlerFactory;
import com.sap.conn.idoc.jco.JCoIDocServer;
import com.sap.conn.idoc.jco.JCoIDocServerContext;
import com.sap.conn.jco.examples.server.beginner.ServerNameConcept;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import com.sap.conn.jco.server.JCoServerTIDHandler;

/**
 * This example shows how a JCoIDocServer can receive an IDoc and store it into a XML file. For JCo functionality description read
 * the JCo examples.
 */
public class IDocServerExample
{
    public static void main(String[] a)
    {
        try
        {
            JCoIDocServer server=JCoIDoc.getServer(ServerNameConcept.SomeSampleServers.SERVER_NAME1);
            server.setIDocHandlerFactory(new MyIDocHandlerFactory());
            server.setTIDHandler(new MyTidHandler());

            MyThrowableListener listener=new MyThrowableListener();
            server.addServerErrorListener(listener);
            server.addServerExceptionListener(listener);
            server.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static class MyIDocReceiveHandler implements JCoIDocHandler
    {
        @Override
        public void handleRequest(JCoServerContext serverCtx, IDocDocumentList idocList)
        {
            FileOutputStream fos=null;
            OutputStreamWriter osw=null;
            try
            {
                IDocXMLProcessor xmlProcessor=JCoIDoc.getIDocFactory().getIDocXMLProcessor();
                fos=new FileOutputStream(serverCtx.getTID()+"_idoc.xml");
                osw=new OutputStreamWriter(fos, "UTF8");
                xmlProcessor.render(idocList, osw, IDocXMLProcessor.RENDER_WITH_TABS_AND_CRLF);
                osw.flush();
            }
            catch (Throwable thr)
            {
                thr.printStackTrace();
            }
            finally
            {
                try
                {
                    if (osw!=null)
                        osw.close();
                    if (fos!=null)
                        fos.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    static class MyIDocHandlerFactory implements JCoIDocHandlerFactory
    {
        private JCoIDocHandler handler=new MyIDocReceiveHandler();

        public JCoIDocHandler getIDocHandler(JCoIDocServerContext serverCtx)
        {
            return handler;
        }
    }

    static class MyThrowableListener implements JCoServerErrorListener, JCoServerExceptionListener
    {

        public void serverErrorOccurred(JCoServer server, String connectionId, JCoServerContextInfo ctx, Error error)
        {
            System.out.println(">>> Error occured on "+server.getProgramID()+" connection "+connectionId);
            error.printStackTrace();
        }

        public void serverExceptionOccurred(JCoServer server, String connectionId, JCoServerContextInfo ctx, Exception error)
        {
            System.out.println(">>> Error occured on "+server.getProgramID()+" connection "+connectionId);
            error.printStackTrace();
        }
    }

    static class MyTidHandler implements JCoServerTIDHandler
    {
        public boolean checkTID(JCoServerContext serverCtx, String tid)
        {
            System.out.println("checkTID called for TID="+tid);
            return true;
        }

        public void confirmTID(JCoServerContext serverCtx, String tid)
        {
            System.out.println("confirmTID called for TID="+tid);
        }

        public void commit(JCoServerContext serverCtx, String tid)
        {
            System.out.println("commit called for TID="+tid);
        }

        public void rollback(JCoServerContext serverCtx, String tid)
        {
            System.out.print("rollback called for TID="+tid);
        }
    }
}
