import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.*;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import org.checkerframework.checker.units.qual.C;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

    public class DriveDocClass {
        private static final String APPLICATION_NAME = "Drive and Doc Application";
        private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
        private static final String TOKENS_DIRECTORY_PATH = "tokens";
        private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
        private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

        private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
                throws IOException {
            // Load client secrets.
            InputStream in = DriveDocClass.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
            }
            GoogleClientSecrets clientSecrets =
                    GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline").build();
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
            //returns an authorized Credential object.
            return credential;
        }

        public static void main(String[] args) {
            try {
                final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                Drive driveService = new Drive.Builder(HTTP_TRANSPORT , JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                Docs docsService = new Docs.Builder(HTTP_TRANSPORT , JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();

                // Create a new Google Docs file
                String documentId = createNewDocument(docsService);

                // List files in Google Drive
                listDriveFiles(driveService);

                // Update the contents of the document
                updateDocumentContent(docsService, documentId);

                // Upload a file to Google Drive
                uploadFileToDrive(driveService, "./Internship.docx");



            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }
        }

        private static String createNewDocument(Docs docsService) throws IOException {
            Document document = new Document().setTitle("Final Document");
            document = docsService.documents().create(document).execute();
            System.out.println("Created new document: " + document.getDocumentId());
            return document.getDocumentId();
        }

        private static void updateDocumentContent(Docs docsService, String documentId) throws IOException {
            List<Request> requests = Collections.singletonList(
                    new Request()
                            .setInsertText(new InsertTextRequest()
                                    .setText("Hello, World!")
                                    .setLocation(new Location().setIndex(1)) // Insert at the beginning
                            )
            );

            BatchUpdateDocumentRequest batchUpdateRequest = new BatchUpdateDocumentRequest().setRequests(requests);
            docsService.documents().batchUpdate(documentId, batchUpdateRequest).execute();
            System.out.println("Updated document content.");
        }

        private static void uploadFileToDrive(Drive driveService, String filePath) throws IOException {
            File fileMetadata = new File().setName("My File");
            java.io.File localFile = new java.io.File(filePath);
            FileContent mediaContent = new FileContent("text/plain", localFile);
            File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            System.out.println("Uploaded file: " + uploadedFile.getId());
        }

        private static void listDriveFiles(Drive driveService) throws IOException {
            FileList fileList = driveService.files().list().setPageSize(50).execute();
            List<File> files = fileList.getFiles();
            if (files != null && !files.isEmpty()) {
                System.out.println("Files in Google Drive:");
                for (File file : files) {
                    System.out.println(file.getName());
                }
            } else {
                System.out.println("No files found in Google Drive.");
            }
        }
    }


