package org.example;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.*;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DoctoHtml {

    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "Google Docs API Java Quickstart";
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String DOCUMENT_ID = "1Eoxua9HKtgzhpFo73u7FV0LH96iTH6VGUUpIQLGsknI";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES =
            Collections.singletonList(DocsScopes.DOCUMENTS_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = org.example.DoctoHtml.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        //returns an authorized Credential object.
        return credential;
    }
    private static String readParagraphElement(ParagraphElement element) {
        TextRun run = element.getTextRun();
        String content=null;
        if (run == null || run.getContent() == null) {
            // The TextRun can be null if there is an inline object.
            return "";
        }
        else content=run.getContent();
        if ( run.getTextStyle()==null || (run.getTextStyle().getBold() == null
                && run.getTextStyle().getItalic() == null && run.getTextStyle().getUnderline() == null)) {
            return content;
        }
        else {
            if(run.getTextStyle().getBold()!=null) {
                content=("<strong>"+content+"</strong>");
                if(run.getTextStyle().getItalic()!=null) {
                    content=("<em>"+content+"</em>");
                    if (run.getTextStyle().getUnderline() != null){
                        content=("<ins>"+content+"</ins>");
                    }
                }
            }
        }
        return content;
    }


    /**
     * Recurses through a list of Structural Elements to read a document's text where text may be in
     * nested elements.
     *
     * @param elements a list of Structural Elements
     */
    private static String readStructuralElements(List<StructuralElement> elements) {
        StringBuilder sb = new StringBuilder();
        for (StructuralElement element : elements) {
            if (element.getParagraph() != null) {
                for (ParagraphElement paragraphElement : element.getParagraph().getElements()) {
                    sb.append(readParagraphElement(paragraphElement));
                }
            } else if (element.getTable() != null) {
                // The text in table cells are in nested Structural Elements and tables may be
                // nested.
                for (TableRow row : element.getTable().getTableRows()) {
                    for (TableCell cell : row.getTableCells()) {
                        sb.append(readStructuralElements(cell.getContent()));
                    }
                }
            }
        }
        return sb.toString();
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Docs service = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        Document response = service.documents().get(DOCUMENT_ID).execute();
        String title = response.getTitle();
        Body body= response.getBody();
        List <StructuralElement> content = new ArrayList<>();
        StringBuilder menu = new StringBuilder();
        StringBuilder container = new StringBuilder();
        int id=0;
        int list=1;
        String bodyTitle = null;
        String askTitle=null;
        for(int i = 0; i < body.getContent().size();i++){
            content.add(body.getContent().get(i));
        }
        for (int i=0; i< content.size(); i++) {
            try {
                StringBuilder sb = new StringBuilder();
                List<StructuralElement> tableContent = null;
                if (content.get(i).getTable() != null) {
                    Color color = null;
                    for (TableRow row : content.get(i).getTable().getTableRows()) {
                        for (TableCell cell : row.getTableCells()) {
                            sb.append(readStructuralElements(cell.getContent()));
                            color = cell.getTableCellStyle().getBackgroundColor().getColor();
                            tableContent = (cell.getContent());
                        }

                    }

                    if (color == null) {
                        container.append("<div class=\"block-code\">");
                        container.append("<pre><code>" + sb + "</code></pre>");
                        container.append("</div>");
                    }else {
                        if (color.getRgbColor().getGreen() > 0.9) {
                            container.append("<aside class=\"special\"><p>" + sb + "</p></aside>");
                        } else if (color.getRgbColor().getRed() > 0.9) {
                            container.append("<aside class=\"warning\"><p>" + sb + "</p></aside>");
                        } else {
                            for (int p = 0; p < tableContent.size(); p++) {
                                StringBuilder t = new StringBuilder();
                                for (int q = 0; q < tableContent.get(p).getParagraph().getElements().size(); q++) {
                                    t.append(readParagraphElement(tableContent.get(p).getParagraph().getElements().get(q)));
                                }
                                if (tableContent.get(p).getParagraph().getBullet() != null) {

                                    container.append("<div class=\"survey-question-options\">");
                                    container.append("<label class=\"survey-option-wrapper\" id=\""+t+"\" for=\"tiu--cu-hi--text\">");
                                    container.append("<span class=\"option-text\">" + t + "</span>");
                                    container.append("<input type=\"radio\" id=\""+t+"\" name=\""+askTitle+"\">");
                                    container.append("<span class=\"custom-radio-button\"></span>");
                                    container.append("</label>\n" +
                                            "            </div>");
                                    if (p == tableContent.size() - 1) {
                                        container.append("</div>\n" +
                                                "    </div>\n" +
                                                "</google-codelab-survey>");
                                    }
                                } else {
                                    askTitle= String.valueOf(t);
                                    container.append("<google-codelab-survey survey-id=\""+bodyTitle+"\" upgraded=\"\">");
                                    container.append("<div class=\"survey-questions\" survey-name=\""+bodyTitle+"\">");
                                    container.append(" <div class=\"survey-question-wrapper\">");
                                    container.append("<h4>" + t + "</h4>");
                                }
                            }
                        }

                    }
                }
                else {
                    List<ParagraphElement> element = content.get(i).getParagraph().getElements();
                    for (ParagraphElement paragraphElement : element) {
                        sb.append(readParagraphElement(paragraphElement));
                    }
                    enum NameStyleType {TITLE, HEADING_1, HEADING_2, HEADING_3, HEADING_4, HEADING_5, HEADING_6, NORMAL_TEXT;}
                    NameStyleType nameStyleType = NameStyleType.valueOf(content.get(i).getParagraph().getParagraphStyle().getNamedStyleType());

                    switch (nameStyleType) {
                        case TITLE -> {
                            System.out.printf("<title>" + title + "</title>\n");
                            bodyTitle= String.valueOf(sb);
                            container.append("<div id=\"codelab-title\">\n" +
                                    "                <div id=\"codelab-nav-buttons\">\n" +
                                    "                    <a href=\"/\" id=\"arrow-back\">\n" +
                                    "                        <i class=\"material-icons\">close</i>\n" +
                                    "                    </a>\n" +
                                    "                    <a href=\"#\" id=\"menu\">\n" +
                                    "                        <i class=\"material-icons\">menu</i>\n" +
                                    "                    </a>\n" +
                                    "                </div>");
                            container.append("<h1 is-upgraded=\"\" class=\"title\">"+bodyTitle+"</h1>\n" +
                                    "            </div><div id=\"main\">");
                        }
                        case HEADING_1 -> {
                            menu.append("\n<li><a href=\"#"+id+"\"> <span class=\"step\">" + sb + "</span></a></li>");
                            if(id!=0){
                                container.append("</google-codelab-step>");
                            }
                            container.append("<google-codelab-step label=\""+sb+"\" duration=\"0\" step=\""+ ++id +"\" style=\"transform: translate3d(0px, 0px, 0px);\">");
                        }
                        case HEADING_2 -> container.append("<h2>" + sb + "</h2>\n");
                        case HEADING_3 -> container.append("<h3>" + sb + "</h3>\n");
                        case HEADING_4 -> container.append("<h4>" + sb + "</h4>\n");
                        case HEADING_5 -> container.append("<h5>" + sb + "</h5>\n");
                        case HEADING_6 -> container.append("<h6>" + sb + "</h6>\n");
                        case NORMAL_TEXT -> {
                            if (content.get(i).getParagraph().getBullet() != null) {
                                if (list==1){
                                    container.append("<ul>");
                                    list=2;
                                }
                                container.append("<li>" + sb + "</li>");
                                if (content.get(i+1).getParagraph().getBullet() == null){
                                    list=1;
                                    container.append("</ul>");
                                }
                            } else {
                                container.append("<p>" + sb + "</p>");
                            }
                        }
                        default -> System.out.println("a");
                    }
                }

            } catch (NullPointerException e) {
                System.out.println();
            }
        }
        //Create the html file
        try {
            FileWriter fileWriter = new FileWriter("index.html");
            fileWriter.write("<!DOCTYPE html>");
            fileWriter.write("<html>");
            fileWriter.write("<head>");
            fileWriter.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
            fileWriter.write("<link rel=\"stylesheet\" href=\"./codelab-elements.css\">\n" +
                    "        <link rel=\"stylesheet\" href=\"./prettify.css\">");
            fileWriter.write("<title>" +bodyTitle+ "</title>\n");
            fileWriter.write("<link rel=\"stylesheet\" href=\"//fonts.googleapis.com/css?family=Source+Code+Pro:400|Roboto:400,300,400italic,500,700|Roboto+Mono\">\n" +
                    "        <link rel=\"stylesheet\" href=\"//fonts.googleapis.com/icon?family=Material+Icons\">");
            fileWriter.write("</head>\n");
            fileWriter.write("<body>");
            fileWriter.write("<lab class=\"slide\" value=\""+ response.getDocumentId() + "\">");
            fileWriter.write("<google-codelab " +
                    "id=\"" + bodyTitle + "\"" +
                    "environment=\"web\" feedback-link=\"\" no-arrows=\"true\"" +
                    "selected=\"0\" " +
                    "google-codelab-ready=\"\" " +
                    "codelab-title=\"" + bodyTitle + "\">");
            fileWriter.write("<div id=\"drawer\">\n" +
                    "            <div class=\"steps\"><ol>"+menu+"</ol></div></div>");
            fileWriter.write(""+container);
            //fileWriter.write("<div class=\"inner\">"+container+"</div>");
            fileWriter.write("</google-codelab-step></div></google-codelab></lab>");
            fileWriter.write("<script src=\"./native-shim.js\"></script>\n" +
                    "        <script src=\"./prettify.js\"></script>\n" +
                    "        <script src=\"./codelab-elements.js\"></script>\n" +
                    "        <script src=\"//support.google.com/inapp/api.js\"></script>");
            fileWriter.write("</body>");

            fileWriter.write("</html>");
            fileWriter.close();
        } catch (FileNotFoundException e) {
            System.out.println("Error");
        }
    }
}