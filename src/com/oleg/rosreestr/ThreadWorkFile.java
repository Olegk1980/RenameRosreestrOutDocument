package com.oleg.rosreestr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class ThreadWorkFile implements Runnable{
	private final Path path;
	private final HashMap<String, String[]> xmlParam = new HashMap<>();	
	
	public ThreadWorkFile(Path path) {
		this.path = path;
		xmlParam.put("Cadastral_Block", new String[] { "", "CadastralNumber" });
		xmlParam.put("CadastralBlock", new String[] { "", "CadastralNumber" });
		xmlParam.put("Parcel", new String[] { "(ЗУ)", "CadastralNumber" });
		xmlParam.put("Building", new String[] { "(Здание)", "CadastralNumber" });
		xmlParam.put("Construction", new String[] { "(Сооружение)", "CadastralNumber" });
		xmlParam.put("Uncompleted_Construction", new String[] { "(Незавершенное сооружение)", "CadastralNumber" });
		xmlParam.put("Uncompleted", new String[] { "(Незавершенное строение)", "CadastralNumber" });
		xmlParam.put("Flat", new String[] { "(Помещение)", "CadastralNumber" });
		xmlParam.put("CadastralNumber", new String[] { "(ОШИБКА)", "" });
	}

	public void run() {
		Path unzipFile = path;				
		while (unzipFile.getFileName().toString().endsWith(".zip")) {
			unzipFile = UnZipFile(unzipFile);
		}

		if (unzipFile.getFileName().toString().endsWith(".xml")) {
			ArrayList<String> valueXML = ParseXML(unzipFile, xmlParam);
			String nameFileUnextension = valueXML.get(0).replace(":", "-") + valueXML.get(1);
			ZipFile(unzipFile, nameFileUnextension);
			System.out.println("--> Обрабатываем пакет: " + path.getFileName().toString() + " ---> получен объект: " + nameFileUnextension + ".xml");
		} else {
			System.out.println("-----> xml фаил ненайден в данном пакете!!!");
		}		
	}
	
	private Path UnZipFile(Path fname) {
		Path temparyFile = null;
		try (ZipInputStream zin = new ZipInputStream(new FileInputStream(fname.toFile()))) {
			ZipEntry entry;
			while ((entry = zin.getNextEntry()) != null) {
				if (entry.getName().endsWith(".zip") || entry.getName().endsWith(".xml")) {
					temparyFile = Files
							.createFile(Paths.get(fname.getParent().toString() + File.separator + entry.getName()));
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

	private void ZipFile(Path fname, String nameFileUnextension) {
		Path pathFileZip = fname.getParent().resolve(RenameRRXML.datePath + File.separator + nameFileUnextension + ".zip");

		Map<String, String> env = new HashMap<>();
		env.put("create", "true");
		env.put("encoding", "CP866");

		URI uri = URI.create("jar:" + pathFileZip.toUri().toString());
		
		try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
			Path inZipfile = zipfs.getPath(nameFileUnextension + ".xml");
			Files.copy(fname, inZipfile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ArrayList<String> ParseXML(Path fname, HashMap<String, String[]> param) {
		ArrayList<String> value = new ArrayList<String>();
		try {
		FileInputStream fis = new FileInputStream(fname.toFile());
		XMLStreamReader xmlr = XMLInputFactory.newInstance().createXMLStreamReader(fname.toString(), fis);
		while (xmlr.hasNext()) {
			xmlr.next();
			if (xmlr.isStartElement()) {				
				if (param.containsKey(xmlr.getLocalName())) {
					if (param.get(xmlr.getLocalName())[0].equals("(ОШИБКА)")) {
						System.out.print(" --->\tВНИМАНИЕ ОШИБКА");
					}
					if (param.get(xmlr.getLocalName())[1].isEmpty()) {
						value.add(xmlr.getElementText());
						value.add(param.get(xmlr.getLocalName())[0]);
						break;
					} else {
						value.add(xmlr.getAttributeValue(null, param.get(xmlr.getLocalName())[1]));
						value.add(param.get(xmlr.getLocalName())[0]);
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
