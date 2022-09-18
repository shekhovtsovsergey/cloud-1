package com.geekbrains;

import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileHandler implements Runnable {

    private static final String SERVER_DIR = "server_files";
    private static final String SEND_FILE_COMMAND = "file";
    private static final Integer BATCH_SIZE = 256;
    private final Socket socket;
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private byte[] batch;
    private String currentDirectory;
    private static final String GET_FILE_COMMAND = "get";




    public FileHandler(Socket socket) throws IOException {
        this.socket = socket;
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        batch = new byte[BATCH_SIZE];
        File file = new File(SERVER_DIR);
        //Получаем список файлов и отправляем его
        setCurrentDirectory(System.getProperty("user.home"));
        send(String.valueOf(getFiles(currentDirectory)));
        if (!file.exists()) {
            file.mkdir();
        }
        System.out.println("Client accepted...");
    }




    @Override
    public void run() {
        try {
            System.out.println("Start listening...");
            while (true) {
                String command = dis.readUTF();

                //Получение файла на сервер
                if (command.equals(SEND_FILE_COMMAND)) {
                    String fileName = dis.readUTF();
                    long size = dis.readLong();
                    try (FileOutputStream fos = new FileOutputStream(SERVER_DIR + "/" + fileName)) {
                        for (int i = 0; i < (size + BATCH_SIZE - 1) / BATCH_SIZE; i++) {
                            int read = dis.read(batch);
                            fos.write(batch, 0, read);
                        }
                    } catch (Exception ignored) {}
                //Отправка файла с сервера
                } else if (command.equals(GET_FILE_COMMAND)) {
                    System.out.println(GET_FILE_COMMAND);
                    String fileName = dis.readUTF();
                    sendToClient(fileName);
                } else {
                    System.out.println("Unknown command received: " + command);
                }
            }
        } catch (Exception ignored) {
            System.out.println("Client disconnected...");
        }
    }


    //Отправка файлов
    public void send(String msg) {
        try {
            dos.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Чтение списка папки
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

    //Адрес папки
    private void setCurrentDirectory(String directory) {
        currentDirectory = directory;
        getFiles(currentDirectory);
    }

    //Отправка файла клиенту
    public void sendToClient(String fileName) {
        System.out.println("sendToClient " + fileName);
        String filePath = currentDirectory + "/" + fileName;
        File file = new File(filePath);
        if (file.isFile()) {
            try {
                System.out.println("sendToClient2 = " + fileName);
                dos.writeUTF(SEND_FILE_COMMAND);
                dos.writeUTF(fileName);
                dos.writeLong(file.length());
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] bytes = fis.readAllBytes();
                    System.out.println(bytes);
                    dos.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                System.err.println("e = " + e.getMessage());
            }
        }
    }



}
