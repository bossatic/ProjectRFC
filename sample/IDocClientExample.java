package com.sap.conn.idoc.examples;

import java.io.BufferedReader;
import java.io.FileReader;

import com.sap.conn.idoc.IDocDocument;
import com.sap.conn.idoc.IDocDocumentList;
import com.sap.conn.idoc.IDocFactory;
import com.sap.conn.idoc.IDocRepository;
import com.sap.conn.idoc.IDocSegment;
import com.sap.conn.idoc.IDocXMLProcessor;
import com.sap.conn.idoc.jco.JCoIDoc;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.examples.client.beginner.DestinationConcept;

/**
 * This example shows how to send an existing IDoc from a given repository as well as an IDoc which is saved in a XML format.
 * For JCo functionality description read the JCo examples.
 */
public class IDocClientExample
{

    public static void main(String[] args)
    {
        try
        {
            JCoDestination destination=JCoDestinationManager.getDestination(DestinationConcept.SomeSampleDestinations.ABAP_AS1);
            IDocRepository iDocRepository=JCoIDoc.getIDocRepository(destination);
            String tid=destination.createTID();
            IDocFactory iDocFactory=JCoIDoc.getIDocFactory();

            // a) create new IDoc
            IDocDocument doc=iDocFactory.createIDocDocument(iDocRepository, "SYIDOC01");
            IDocSegment segment=doc.getRootSegment();
            segment=segment.addChild("E1IDOCH");
            // and so on. See IDoc Specification .....
            JCoIDoc.send(doc, IDocFactory.IDOC_VERSION_DEFAULT, destination, tid);

            // b) use existent xml file
            String iDocXML=getXMLFromFile("syidoc01.xml");

            IDocXMLProcessor processor=iDocFactory.getIDocXMLProcessor();
            IDocDocumentList iDocList=processor.parse(iDocRepository, iDocXML);
            JCoIDoc.send(iDocList, IDocFactory.IDOC_VERSION_DEFAULT, destination, tid);
            destination.confirmTID(tid);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static String getXMLFromFile(String filePath)
    {
        String iDocXML=null;
        FileReader fileReader;

        try
        {
            fileReader=new FileReader(filePath);
            BufferedReader br=new BufferedReader(fileReader);
            StringBuffer sb=new StringBuffer();
            String line;
            while ((line=br.readLine())!=null)
            {
                sb.append(line);
            }
            iDocXML=sb.toString();

            br.close();
            fileReader.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return iDocXML;
    }
}
