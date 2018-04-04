package sg.edu.ntu.hospitalbeesqdemo.repository;

import org.springframework.stereotype.Component;

import java.io.*;

public class SerializationUtil {
    /** Serializes the class to a particular location.
     * @param toSave The class to be serialized.
     * @param filename The file name for the file
     *
     * @return True if successful and false if it is not.
     */
    public static void saveObject(Object toSave, String filename) throws IOException
    {
        FileOutputStream fileOut = new FileOutputStream(filename);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(toSave);
        out.flush();
        out.close();
        fileOut.close();
    }

    /** Deserializes the class from a particular file.
     *
     * @param filename The file name of the file.
     *
     * @return The object that has been unserialized.
     */
    public static Object loadObject(String filename)
    {
        Object toOpen;

        try
        {
            FileInputStream fileIn = new FileInputStream(filename);
            ObjectInputStream read = new ObjectInputStream(fileIn);
            toOpen = read.readObject();
            read.close();
            fileIn.close();

            return toOpen;
        }
        catch(Exception e)
        {
            return null;
        }
    }
}
