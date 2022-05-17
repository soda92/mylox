import java.util.*;
import java.io.*;

public class Read{
    public static void main(String[] args) throws IOException{
        var in = new InputStreamReader(System.in);
        var reader = new BufferedReader(in);
        var str = reader.readLine();
        if(str.equals(""+(char)0x04)){
            System.out.println("true");
        }
        else{
            System.out.print((int)str.charAt(0));
        }
    }
}
