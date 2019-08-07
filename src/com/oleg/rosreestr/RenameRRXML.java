package com.oleg.rosreestr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class RenameRRXML {
	final static String datePath = new SimpleDateFormat("yyyy.MM.dd").format(new Date());
	
	public static void main(String[] args) {
        Path currentPath = null;        
        String maskNameZip = "Response*.zip";
        HashMap<String, String> xmlElements = new HashMap<>();
        xmlElements.put("Cadastral_Block", "");
        xmlElements.put("CadastralBlock", "");
        xmlElements.put("Parcel", "(ЗУ)");
        xmlElements.put("Building", "(Здание)");
        xmlElements.put("Construction", "(Сооружение)");
        xmlElements.put("Uncompleted_Construction", "(Незавершенное сооружение)");
        xmlElements.put("Uncompleted", "(Незавершенное строение)");
        xmlElements.put("Flat", " (Помещение)");
        try {            
            currentPath = Paths.get(args[0]).normalize().toAbsolutePath();       
        } catch (ArrayIndexOutOfBoundsException e) {
            currentPath = Paths.get("").toAbsolutePath();
        } finally {
            System.out.println("Рабочий каталог: " +  currentPath);
            int count = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath, maskNameZip)){
            	if (!Files.exists(currentPath.resolve(datePath), new LinkOption[]{ LinkOption.NOFOLLOW_LINKS}))
            	{
            		Files.createDirectory(currentPath.resolve(datePath));
            	}
            	            	            	
                for (Path entry: stream) 
                {                	
                    System.out.print("--> Обрабатываем пакет: "+entry.getFileName().toString());                    
                    Path unzipFile = entry;
                    while (unzipFile.getFileName().toString().endsWith(".zip")) 
                    {
                    	unzipFile = UnZipFile(unzipFile);
                    }
                    if (unzipFile.getFileName().toString().endsWith(".xml")) 
                    {
                    	ArrayList<String> valueXML = ParseXML(unzipFile, xmlElements, "CadastralNumber");
                    	String nameFileUnextension = valueXML.get(0).replace(":", "-")+valueXML.get(1);
                    	ZipFile(unzipFile, nameFileUnextension);
                    	System.out.println(" ---> получен объект: " + nameFileUnextension + ".xml");						                     	
                    } 
                    else 
                    {
                    	System.out.println("-----> xml фаил ненайден в данном пакете!!!");
                    }
                    count ++;
                }
                System.out.println("Всего обработано пакетов: "+count);
            } catch (Exception e) {
                e.printStackTrace();
            }            
        }
	}
	
	private static Path UnZipFile(Path fname) {
        Path temparyFile = null;
        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(fname.toFile()))){
            ZipEntry entry;
            while((entry = zin.getNextEntry()) != null) {              
                if (entry.getName().endsWith(".zip") || entry.getName().endsWith(".xml")) {
                    temparyFile = Files.createFile(Paths.get(fname.getParent().toString()+File.separator+entry.getName()));
                    temparyFile.toFile().deleteOnExit();
                    FileOutputStream fout = new FileOutputStream(temparyFile.toFile());                    
                    for (int c = zin.read(); c != -1; c = zin.read()) {
                        fout.write(c);
                    }
                    fout.flush();
                    zin.closeEntry();
                    fout.close();
                }                
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return temparyFile;
    }
	
	private static void ZipFile(Path fname, String nameFileUnextension) {		
		Path pathFileZip = fname.getParent().resolve(datePath + File.separator + nameFileUnextension + ".zip");
		
		Map<String, String> env = new HashMap<>(); 
        env.put("create", "true");
        env.put("encoding", "CP866");

        URI uri = URI.create("jar:" + pathFileZip.toUri().toString());        
        		
       try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
    	    Path inZipfile = zipfs.getPath(nameFileUnextension + ".xml");
            Files.copy(fname, inZipfile, StandardCopyOption.REPLACE_EXISTING ); 
        } catch (IOException e) {
			e.printStackTrace();
		}
    }
	
	private static ArrayList<String> ParseXML(Path fname, HashMap<String, String> element, String attribute)
	{
		ArrayList<String> value = new ArrayList<String>();
		try {
			FileInputStream fis = new FileInputStream(fname.toFile());
			XMLStreamReader xmlr = XMLInputFactory.newInstance().createXMLStreamReader(fname.toString(), fis);
			while (xmlr.hasNext()) {
				xmlr.next();
				if (xmlr.isStartElement()) {
					if (element.containsKey(xmlr.getLocalName())) {
						if (attribute.isEmpty()) {
							value.add(xmlr.getElementText());
							value.add(element.get(xmlr.getLocalName()));
							break;
						} else {
							value.add(xmlr.getAttributeValue(null, attribute));
							value.add(element.get(xmlr.getLocalName()));
							break;
						}
					}
				}
			}
			xmlr.close();
			fis.close();
		} catch (IOException | XMLStreamException ex) {
			ex.printStackTrace();
		}
		return value;
	}
}