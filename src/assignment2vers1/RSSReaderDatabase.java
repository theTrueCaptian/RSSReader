

package assignment2vers1;

/*     	 Name:	  Maeda Hanafi
 	 Course:	   463
 	 Assignment:	   #2
  */


import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.time.TimeTCPClient;


public class RSSReaderDatabase {
    public class RSSNode{
        String title;
        String link;
        String pubDate;
        public RSSNode(String title, String link, String pubDate){
            this.title = title; this.link = link; this.pubDate = pubDate;
        }
    }
    ArrayList<RSSNode> buffer = new ArrayList<RSSNode>();
    long time;
    private static RSSReaderDatabase instance = null;

    private RSSReaderDatabase() {    }

    public static RSSReaderDatabase getInstance() {
        if(instance == null)
            instance = new RSSReaderDatabase();
        return instance;
    }

    public void writeNews() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            URL u = new URL("http://rss.cnn.com/rss/cnn_allpolitics.rss"); // your feed url
            Document doc = builder.parse(u.openStream());
            NodeList nodes = doc.getElementsByTagName("item");

            for(int i=0;i<nodes.getLength();i++) {
                Element element = (Element)nodes.item(i);
                String title = getElementValue(element,"title");
                String link = getElementValue(element,"link");
                String pubDate =  getElementValue(element,"pubDate");
                buffer.add(new RSSNode(title, link, pubDate));
                System.out.println("Title: " + title);
                System.out.println("Link: " + link);
                System.out.println("Publish Date: " + pubDate);

                System.out.println();
            }
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getCharacterDataFromElement(Element e) {
        try {
            Node child = e.getFirstChild();
            if(child instanceof CharacterData) {
                CharacterData cd = (CharacterData) child;
                return cd.getData();
            }
        }catch(Exception ex) {        }
        return "";
    } 

    private String getElementValue(Element parent,String label) {
        return getCharacterDataFromElement((Element)parent.getElementsByTagName(label).item(0));
    }

    
    public void getTime(){
        System.out.println("Getting time from nistServers");
        try {
            String TIME_SERVER = "nist1-ny.ustiming.org";
            NTPUDPClient timeClient = new NTPUDPClient();
            TimeTCPClient timeClientTCP = new TimeTCPClient();
            
            InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);
            
            timeClientTCP.connect(inetAddress);
             time = timeClientTCP.getTime();
           /* System.out.println("Connected to the timeserver");
            TimeInfo timeInfo=null;
            try {
                timeInfo = timeClient.getTime(inetAddress);
                System.out.println("Getting the time done");
            } catch (IOException ex) {
                Logger.getLogger(RSSReaderDatabase.class.getName()).log(Level.SEVERE, null, ex);
            }
            long returnTime = timeInfo.getReturnTime();

             time = new Date(returnTime);*/
            
            System.out.println("Time from " + TIME_SERVER + ": " + time);
        } catch (IOException ex) {
            Logger.getLogger(RSSReaderDatabase.class.getName()).log(Level.SEVERE, null, ex);
        } /*catch (UnknownHostException ex) {
            Logger.getLogger(RSSReaderDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }
    
    public void databaseConnect(){
           String jdbcDriver = "com.mysql.jdbc.Driver";
           
           String databaseTest = "jdbc:mysql://localhost/csc463?" +"user=root&password=";

           String user = "";
           String password = ""; // to be given in class
           String databaseLive = databaseTest;
           System.out.println("***Solution being executed***");
           try
           {  Class.forName(jdbcDriver);
              System.out.println("...Driver loaded");
              DriverManager.setLoginTimeout(0);
              Connection cn = DriverManager.getConnection(databaseLive);
              //Connection cn = DriverManager.getConnection(databaseTest);
              System.out.println("...Connection established");
              java.sql.Statement query =  cn.createStatement();
              System.out.println("...Statement created");

              System.out.println(isTableExist( query, "", "tblCss"));
              String sqlCmd = "tblCss(cssid int not null auto_increment, title varchar(500), link varchar(500), pubDate varchar(500), primary key(cssid) )";
              buildTable(query,"tblCss" ,sqlCmd);
              populateNewsTable(cn, "tblCss");

              String sqlCmd2 = "hanafim1(id int not null auto_increment, newsTable varchar(500), date varchar(500), primary key(id))";
              buildTable(query, "hanafim1", sqlCmd2);
              populateNameTable(cn, "hanafim1", "tblCss");

           }catch (Exception e){System.out.println("Problem accessing the database "+e);}
      }

      public void populateNameTable(Connection conn, String tableName, String prevTable){
        try {
            PreparedStatement pstmt = conn.prepareStatement("insert into " + tableName + "(newsTable, date) values(?, ?)");
            pstmt.setString(1, prevTable);
            pstmt.setString(2, time+"");
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(RSSReaderDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }
      }

      public void populateNewsTable(Connection conn, String tableName){
        for(int i=0; i<buffer.size(); i++){
            try {
                PreparedStatement pstmt = conn.prepareStatement("insert into " + tableName + "(title, link, pubDate) values(?, ?, ?)");
                pstmt.setString(1, buffer.get(i).title);
                pstmt.setString(2, buffer.get(i).link);
                pstmt.setString(3, buffer.get(i).pubDate);
                pstmt.executeUpdate();
               
            } catch (SQLException ex) {
                Logger.getLogger(RSSReaderDatabase.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
      }
    
      public boolean buildTable(java.sql.Statement query, String tableName, String tableStruct){
        if(!isTableExist(query, "", tableName)){
            System.out.println("Success. Creating");
            String sqlCmd = "CREATE TABLE "+tableStruct;
            try {
                query.execute(sqlCmd);
            } catch (SQLException ex) {
                System.out.println("hahah it doesn't work for u! "+ex);
            }
        }else{
            System.out.println("fail");
        }
        return true;
      }
    
      public boolean isTableExist(java.sql.Statement query, String catalogName, String tableName){
        try {
            String sqlCmd = "SHOW TABLES LIKE '" + tableName+"'";
            ResultSet rs = query.executeQuery(sqlCmd);
         
            if(rs.next()){
                return true;
            }else
                return false;
        } catch (SQLException ex) {
            Logger.getLogger(RSSReaderDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
      }
      
      public static void main(String[] args) {
        RSSReaderDatabase readerDatabase = RSSReaderDatabase.getInstance();
        readerDatabase.writeNews();
        readerDatabase.getTime();
        readerDatabase.databaseConnect();
      }
}


