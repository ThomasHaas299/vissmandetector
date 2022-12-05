package de.thomashaas.vissmanndetector.http;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * @author thaas, frei nach
 * http://stackoverflow.com/questions/2469451/upload-files-with-java
 */
@SuppressWarnings({"unused", "WeakerAccess", "SameParameterValue"})
public class HttpPost {

	private static final String lineEnd = "\r\n";
	private static final String twoHyphens = "--";

	private final String url;
	private final String boundary;
	private HttpURLConnection conn;
	private Exception exception;
	private boolean requestDone = false;
	private DataOutputStream dataOutputStream = null;
	private int responseCode;
	private boolean responseInputStreamUsed = false;
	private String responseInputStreamUsedMethod;
	private LinkedHashMap<String, String> params;
	private ArrayList<FileStructure> files;
	private boolean isUsingInputStream = false;

	public HttpPost(String pUrl) {
		url = pUrl;
		boundary = Long.toHexString(System.currentTimeMillis());
	}

	public void addParam(String name, int value) {
		addParam(name, String.valueOf(value));
	}

	public void addParam(String name, String value) {
		if (params == null) {
			params = new LinkedHashMap<>();
		}
		params.put(name, value);
	}

	public void addFile(String pKey, String fileName) {
		if (files == null) {
			files = new ArrayList<>();
		}
		files.add(new FileStructure(pKey, fileName));
	}

	public void addFile(String pKey, String fileName, String newFileName) {
		if (files == null) {
			files = new ArrayList<>();
		}
		files.add(new FileStructure(pKey, fileName, newFileName));
	}

	public void addFile(String pKey, InputStream pInputStream, String newFileName) {
		isUsingInputStream = true;
		if (files == null) {
			files = new ArrayList<>();
		}
		files.add(new FileStructure(pKey, pInputStream, newFileName));
	}

	/**
	 * Ausführen der Abfrage.
	 * <p>
	 * Ist optional, wenn statt dessen die Response abgefragt wird. Geht nur einmal.
	 */
	public void doRequest() {
		if (requestDone) {
			return;
		}
		try {
			_doRequest();
//        } catch (MalformedURLException e) {
//            exception = e;
//        } catch (ProtocolException e) {
//            exception = e;
		}
		catch (IOException e) {
			exception = e;
		}
		requestDone = true;

		responseCode = 0;

		if (exception != null) {
			return;
		}

		try {
			responseCode = conn.getResponseCode();
		}
		catch (IOException e) {
			this.exception = e;
		}

	}

	private void _doRequest() throws IOException {

		conn = (HttpURLConnection) new URL(url).openConnection();

		conn.setConnectTimeout(HttpSettings.CONNECTION_TIMEOUT);
		conn.setReadTimeout(HttpSettings.CONNECTION_READ_TIMEOUT);

		// Allow Inputs and Outputs
		conn.setDoInput(true);
		conn.setDoOutput(true);

		conn.setUseCaches(false);

		// This sets request method to POST.
		conn.setRequestMethod("POST");

		conn.setRequestProperty("Connection", "Keep-Alive");
		conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		// conn.setFixedLengthStreamingMode(720000);

		String end = twoHyphens + boundary + twoHyphens + lineEnd;

		if (!isUsingInputStream) {
			int size = end.getBytes().length;
			size += obtainParamsSize();
			size += obtainFilesSize();
			// Statics.logs("_doRequest: size: " + size);
			conn.setFixedLengthStreamingMode(size);
		}

		dataOutputStream = new DataOutputStream(conn.getOutputStream());

		putParams();
		putFiles();

		writeStringToDataOutputStream(end);

		dataOutputStream.flush();
		dataOutputStream.close();

	}

	private void writeStringToDataOutputStream(String s) throws IOException {
		dataOutputStream.write(s.getBytes(StandardCharsets.UTF_8));
	}

	@SuppressWarnings("RedundantThrows")
	private int obtainStringLength(String s) throws UnsupportedEncodingException {
		return s.getBytes(StandardCharsets.UTF_8).length;
	}

	private int obtainParamsSize() throws UnsupportedEncodingException {
		if (params == null) {
			return 0;
		}

		int size = 0;


		String value;

		for (String name : params.keySet()) {
			value = params.get(name);

			if (value == null) {
				continue;
			}

			size += obtainStringLength(twoHyphens + boundary + lineEnd);
			size += obtainStringLength("Content-Disposition: form-data; name=\"" + name + "\"");
			size += obtainStringLength(lineEnd);
			size += obtainStringLength("Content-Type: text/plain; charset=UTF-8");
			size += obtainStringLength(lineEnd);
			size += obtainStringLength(lineEnd);
			size += obtainStringLength(value);
			size += obtainStringLength(lineEnd);
		}

		return size;
	}

	private int obtainFilesSize() throws UnsupportedEncodingException {
		if (files == null) {
			return 0;
		}

		int size = 0;

		for (FileStructure fileStructure : files) {

			if (fileStructure.fileName == null) {
				// das ist bei InputStreams der Fall
				continue;
			}

			File aFile = new File(fileStructure.fileName);
			if (!aFile.exists()) {
				continue;
			}

			size += obtainStringLength(twoHyphens + boundary + lineEnd);
			size += obtainStringLength("Content-Disposition: form-data; name=\"" + fileStructure.getKey() + "\";"
					+ " filename=\"" + fileStructure.getNewFileName() + "\"" + lineEnd);
			size += obtainStringLength(lineEnd);
			size += aFile.length();
			size += obtainStringLength(lineEnd);
		}

		return size;
	}

	private void putParams() throws IOException {
		if (params == null) {
			return;
		}

		String value;

		for (String name : params.keySet()) {
			value = params.get(name);

			if (value == null) {
				continue;
			}

			writeStringToDataOutputStream(twoHyphens + boundary + lineEnd);
			writeStringToDataOutputStream("Content-Disposition: form-data; name=\"" + name + "\"");
			writeStringToDataOutputStream(lineEnd);
			writeStringToDataOutputStream("Content-Type: text/plain; charset=UTF-8");
			writeStringToDataOutputStream(lineEnd);
			writeStringToDataOutputStream(lineEnd);
			writeStringToDataOutputStream(value);
			writeStringToDataOutputStream(lineEnd);

		}
	}

	private void putFiles() throws IOException {
		if (files == null) {
			return;
		}

		InputStream inputStream;

		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		// int maxBufferSize = 131072; // 1024 * 128
		int maxBufferSize = 1048576; // 1024 * 1024

		for (FileStructure fileStructure : files) {

			if (!isUsingInputStream && fileStructure.fileName != null) {
				File aFile = new File(fileStructure.fileName);
				if (aFile.exists()) {
					postFileLength((int) aFile.length());
				}
			}

			inputStream = fileStructure.getInputStream();

			if (inputStream == null) {
				throw new FileNotFoundException("key: " + fileStructure.getKey() + ", newName: "
						+ fileStructure.getNewFileName());
			}

			writeStringToDataOutputStream(twoHyphens + boundary + lineEnd);
			writeStringToDataOutputStream("Content-Disposition: form-data; name=\"" + fileStructure.getKey() + "\";"
					+ " filename=\"" + fileStructure.getNewFileName() + "\"" + lineEnd);
			writeStringToDataOutputStream(lineEnd);

			// create a buffer of maximum size
			bytesAvailable = inputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = new byte[bufferSize];
			int completeBytesWritten = 0;

			// read file and write it into form...
			bytesRead = inputStream.read(buffer, 0, bufferSize);


			while (bytesRead > 0) {
				dataOutputStream.write(buffer, 0, bufferSize);
				if (!isUsingInputStream) {
					completeBytesWritten += bytesRead;
					postBytesWritten(completeBytesWritten);
				}
				bytesAvailable = inputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = inputStream.read(buffer, 0, bufferSize);
				// Statics.logs("putFiles: " + bytesRead);
			}

			writeStringToDataOutputStream(lineEnd);
			inputStream.close();
		}

	}

	@SuppressWarnings("EmptyMethod")
	public void postBytesWritten(int completeBytesWritten) {
		// to be extended
	}

	@SuppressWarnings("EmptyMethod")
	public void postFileLength(int fileSize) {
		// to be extended
	}

	/**
	 * Der Response als String, z.B. HTML, XML, JSON, TXT, ...
	 *
	 * @return String
	 * @throws ResponseInputStreamAlreadyUsedException - yes
	 */
	public String getResponse() {

		doRequest();

		if (exception != null) {
			return "";
		}

		if (responseInputStreamUsed) {
			throw new ResponseInputStreamAlreadyUsedException(responseInputStreamUsedMethod);
		}
		responseInputStreamUsed = true;
		responseInputStreamUsedMethod = "getResponse";

		InputStream in;

		try {
			if (responseCode < 300) {
				in = conn.getInputStream();
			}
			else {
				in = conn.getErrorStream();
			}

			// InputStream in String umwandeln
			// http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
			java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";

		}
		catch (IOException e) {
			this.exception = e;
			return "";
		}
	}

	/**
	 * Should be 200
	 *
	 * @return the ResconseCode
	 */
	public int getResponseCode() {

		doRequest();
		return responseCode;

	}

	/**
	 * Brief ResponseMessage (OK)
	 *
	 * @return the ResponseMessage
	 */
	public String getResponseMessage() {

		doRequest();

		if (exception != null) {
			return "";
		}

		try {
			return conn.getResponseMessage();
		}
		catch (IOException e) {
			this.exception = e;
			return "";
		}
	}

	/**
	 * Liefert den Response-Input-Stream
	 *
	 * @return InputStream
	 * @throws ResponseInputStreamAlreadyUsedException - yes
	 */
	public InputStream getInputStream() {

		doRequest();

		if (exception != null) {
			return null;
		}

		if (responseInputStreamUsed) {
			throw new ResponseInputStreamAlreadyUsedException(responseInputStreamUsedMethod);
		}
		responseInputStreamUsed = true;
		responseInputStreamUsedMethod = "getInputStream";

		try {
			return conn.getInputStream();
		}
		catch (IOException e) {
			this.exception = e;
			return null;
		}
	}

	/**
	 * Schreibt den Response-InputStream zum StdOut. Eher eine exemplarische
	 * Funktion.
	 *
	 * @throws ResponseInputStreamAlreadyUsedException - yes
	 */
	public void writeInputStreamToStdOut() {

		doRequest();

		if (exception != null) {
			return;
		}

		if (responseInputStreamUsed) {
			throw new ResponseInputStreamAlreadyUsedException(responseInputStreamUsedMethod);
		}
		responseInputStreamUsed = true;
		responseInputStreamUsedMethod = "writeInputStreamToStdOut";

		try {
			InputStream in = conn.getInputStream();

			//noinspection UnusedAssignment
			int size = 0;
			byte[] buffer = new byte[1024];
			while ((size = in.read(buffer)) != -1)
				System.out.write(buffer, 0, size);
		}
		catch (IOException e) {
			this.exception = e;
		}

	}

	/**
	 * Überprüft, ob eine Exception geworfen wurde
	 *
	 * @return true, wenn Exception
	 */
	public boolean checkException() {
		return exception != null;
	}

	/**
	 * Gibt die Exception aus, wenn eine aufgetreten ist
	 */
	public void printException() {
		if (exception != null) {
			exception.printStackTrace();
		}
	}

	/**
	 * Liefert die eventuell geworfene Exception
	 *
	 * @return the Exception
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * Wird geworfen, wenn der InputStream der Response mehrfach abgefragt wird.
	 * <p>
	 * Der Input-Stream vom Response kann natürlich nur einmal verwendet werden,
	 * und man muss sich dann schon für eine Möglichkeit entscheiden. Im
	 * einfachsten Fall nimmt man getResponse();
	 *
	 * @author thaas
	 */
	@SuppressWarnings("unused")
	private static class ResponseInputStreamAlreadyUsedException extends RuntimeException {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public ResponseInputStreamAlreadyUsedException() {
			super("Response-InputStream has already been used");
		}

		ResponseInputStreamAlreadyUsedException(String message) {
			super("Response-InputStream has already been used here: " + message);
		}

	}

	/**
	 * Struktur zur Aufnahme der Datei-Daten, die gepostet werden sollen
	 *
	 * @author thaas
	 */
	private class FileStructure {

		private final String key;
		private final String newFileName;
		private String fileName;
		private InputStream inputStream;

		FileStructure(String pKey, String pFileName) {
			key = pKey;
			fileName = pFileName;
			newFileName = new File(fileName).getName();
		}

		FileStructure(String pKey, String pFileName, String pNewFileName) {
			key = pKey;
			fileName = pFileName;
			newFileName = pNewFileName;
		}

		FileStructure(String pKey, InputStream pInputStream, String pNewFileName) {
			key = pKey;
			inputStream = pInputStream;
			newFileName = pNewFileName;
		}

		String getKey() {
			return key;
		}

		String getNewFileName() {
			return newFileName;
		}

		InputStream getInputStream() {
			if (inputStream == null) {
				try {
					inputStream = new FileInputStream(fileName);
				}
				catch (FileNotFoundException e) {
					exception = e;
					// e.printStackTrace();
				}
			}
			return inputStream;
		}

	}

}