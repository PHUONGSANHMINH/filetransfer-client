/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package data;

import io.socket.client.Ack;
import io.socket.client.Socket;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import org.json.JSONException;
import org.json.JSONObject;
/**
 *
 * @author LENOVO
 */
public class DataReader {

    /**
     * @return the fileID
     */
    public int getFileID() {
        return fileID;
    }

    /**
     * @param fileID the fileID to set
     */
    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    /**
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * @return the fileSize
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * @param fileSize the fileSize to set
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the accFile
     */
    public RandomAccessFile getAccFile() {
        return accFile;
    }

    /**
     * @param accFile the accFile to set
     */
    public void setAccFile(RandomAccessFile accFile) {
        this.accFile = accFile;
    }

    public DataReader(File file) throws IOException {
        accFile = new RandomAccessFile(file, "r");
        this.file = file;
        this.fileSize = accFile.length();
        this.fileName = file.getName();
    }
    
    private int fileID;
    private File file;
    private long fileSize;
    private String fileName;
    private RandomAccessFile accFile;
    
    public synchronized byte[] readFile() throws IOException{
        long filePointer = accFile.getFilePointer();
        if(filePointer != fileSize){
            int max = 2000;
            long length = filePointer + max >= fileSize ? fileSize-filePointer:max;
            byte [] data = new byte[max];
            accFile.read(data);
            return data;
        }else{
            return null;
        }
    }
    public void close() throws IOException{
        accFile.close();
    }
    public String getFileSizeConverted(){
        double bytes = fileSize;
        String[] fileSizeUnits = {"bytes", "KB", "MB", "GB", "TB", "FB", "EB", "ZB", "YB"};
        String sizeToReturn;
        DecimalFormat df = new DecimalFormat("0.#");
        int index;
        for (index = 0; index < fileSizeUnits.length; index++){
            if(bytes < 1024){
                break;
            }
            bytes = bytes/1024;
        }
        System.out.println("Systematic file size : "+ bytes + " "+ fileSizeUnits[index]);
        sizeToReturn = df.format(bytes) + " " + fileSizeUnits[index];
        return sizeToReturn;
    }
    public double getPercentage() throws IOException{
        double percentage = 0;
        long filePointer = accFile.getFilePointer();
        percentage = filePointer * 100 / fileSize;
        return percentage;
    }
    public Object[] toRowtable (int no){
        return new Object[]{this, no, fileName, getFileSizeConverted(), "Next update"};
    }
    public void startSend(Socket socket) throws JSONException{
        JSONObject data = new JSONObject();
        data.put("fileName", fileName);
        data.put("fullSize", fileSize);
        socket.emit("send_file", data, new Ack() {
            @Override
            public void call(Object... os) {
                if(os.length > 0){
                    boolean action = (boolean)os[0];
                    if(action){
                        
                        fileID = (int)os[1];
                    }
                }
            }
        });
    }
    private void sendingFile(Socket socket)throws IOException, JSONException{
        JSONObject data = new JSONObject();
        data.put("fileID", fileID);
        byte[] bytes = readFile();
        if(bytes != null){
            data.put("data", bytes );
            data.put("finish", false);
        }else{
            data.put("finish", true);
            close();
        }
        socket.emit("sending", data, new Ack() {
            @Override
            public void call(Object... os) {
                if (os.length > 0){
                    boolean act = (boolean) os[0];
                    if(act){
                        try {
                            sendingFile(socket);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }
}
