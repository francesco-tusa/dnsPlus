package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author uceeftu
 */
public class FileLoader {
    public static List loadNames(String fileName) throws FileNotFoundException 
    {
        File serviceNames = new File(fileName);
        List<String> services = new ArrayList<>();
        
        try (Scanner scanner = new Scanner(serviceNames)) {
            while (scanner.hasNextLine())
            {
                String service = scanner.nextLine();
                services.add(service);
            }
        }
        
        return services;
    }  
}
