package com.geekbrains.sep22.geekcloudclient;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;


public class CloudMainController implements Initializable {

    public ListView<String> clientView;
    public ListView<String> serverView;
    private String currentDirectory;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Socket socket;
    private static final String SEND_FILE_COMMAND = "file";

    //Новые по дз
    public static final String REGEX = ",";
    private static final String GET_FILE_COMMAND = "get";
    private static final String SERVER_DIR = "client_files";
    private static final Integer BATCH_SIZE = 256;
    private byte[] batch;




    public void sendToServer(ActionEvent actionEvent) {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        String filePath = currentDirectory + "/" + fileName;
        File file = new File(filePath);
        if (file.isFile()) {
            try {
                dos.writeUTF(SEND_FILE_COMMAND);
                dos.writeUTF(fileName);
                dos.writeLong(file.length());
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] bytes = fis.readAllBytes();
                    dos.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                System.err.println("e = " + e.getMessage());
            }
        }
    }



    private void initNetwork() {
        try {
            socket = new Socket("localhost", 8190);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            //после соединения вызывается метод readMessages() в котором уже полученное сообщение передается в контроллер, потом методы чтения и парсинга
            readMessages();
            batch = new byte[BATCH_SIZE];

        } catch (Exception ignored) {
        }
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initNetwork();
        setCurrentDirectory(System.getProperty("user.home"));
        fillView(clientView, getFiles(currentDirectory));
        clientView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = clientView.getSelectionModel().getSelectedItem();
                File selectedFile = new File(currentDirectory + "/" + selected);
                if (selectedFile.isDirectory()) {
                    setCurrentDirectory(currentDirectory + "/" + selected);
                }
            }
        });
    }


    private void setCurrentDirectory(String directory) {
        currentDirectory = directory;
        fillView(clientView, getFiles(currentDirectory));
    }


    private void fillView(ListView<String> view, List<String> data) {
        view.getItems().clear();
        view.getItems().addAll(data);
    }


    private List<String> getFiles(String directory) {
        // file.txt 125 b
        // dir [DIR]
        File dir = new File(directory);
        if (dir.isDirectory()) {
            String[] list = dir.list();
            if (list != null) {
                List<String> files = new ArrayList<>(Arrays.asList(list));
                files.add(0, "..");
                return files;
            }
        }
        return List.of();
    }


    //чтение
    public void readMessages() {
        var thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                    var message = dis.readUTF();



                    //Получение файла на сервер
                    if (message.equals(SEND_FILE_COMMAND)) {
                        System.out.println("Hello");
                        String fileName = dis.readUTF();
                        long size = dis.readLong();
                        try (FileOutputStream fos = new FileOutputStream(SERVER_DIR + "/" + fileName)) {
                            for (int i = 0; i < (size + BATCH_SIZE - 1) / BATCH_SIZE; i++) {
                                int read = dis.read(batch);
                                fos.write(batch, 0, read);
                            }
                        } catch (Exception ignored) {}


                    } else {
                        processMessage(message);
                    }


                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }



    public void processMessage(String message) {
        Platform.runLater(() -> {
            try {
                parseIncomingMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    //парсинг
    private void parseIncomingMessage(String message) throws IOException {


        var splitMessage = message.split(REGEX);

       
            List<String> files = new ArrayList();
       
            getFiles(String.valueOf(files));


            for (int i = 0; i < splitMessage.length; i++) {
                files.add(splitMessage[i]);
            }

            fillView(serverView, files);
       

        }



    public void getFromServer(ActionEvent actionEvent) {
        String fileName_ = serverView.getSelectionModel().getSelectedItem();
        String fileName = fileName_.substring(1);
            try {
                dos.writeUTF(GET_FILE_COMMAND);
                dos.writeUTF(fileName);
            } catch (Exception e) {
                System.err.println("e = " + e.getMessage());
            }
        }



    }


