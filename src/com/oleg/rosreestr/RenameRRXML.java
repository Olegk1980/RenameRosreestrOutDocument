package com.oleg.rosreestr;

import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class RenameRRXML {
	final static String datePath = new SimpleDateFormat("yyyy.MM.dd").format(new Date());

	public static void main(String[] args) {
		Path currentPath = null;
		String maskNameZip = "Response*.zip";
				 
		try {
			currentPath = Paths.get(args[0]).normalize().toAbsolutePath();
		} catch (ArrayIndexOutOfBoundsException e) {
			currentPath = Paths.get("").toAbsolutePath();
		} finally {
			System.out.println("Рабочий каталог: " + currentPath);
			int count = 0;
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath, maskNameZip)) {
				if (!Files.exists(currentPath.resolve(datePath), new LinkOption[] { LinkOption.NOFOLLOW_LINKS })) {
					Files.createDirectory(currentPath.resolve(datePath));
				}			

				Thread th = null;
				for (Path path : stream) {
					th = new Thread(new ThreadWorkFile(path));
					th.start();
					if (Thread.activeCount() >= 6) {
						th.join();
					}
					count++;
				}
				while (Thread.activeCount() > 1) {
					th.join();
				}
				System.out.println("Всего обработано пакетов: " + count);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
}