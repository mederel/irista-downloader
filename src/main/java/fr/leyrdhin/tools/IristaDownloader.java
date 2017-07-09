package fr.leyrdhin.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * Created by kspx on 09/07/17.
 */
public class IristaDownloader {
    private static final String ALBUM_OPTION = "a";
    private static final String DESTINATION_FOLDER_OPTION = "d";
    private static final Pattern P_ALBUM_ID = Pattern.compile("^.*/gallery/(\\w+)\\W.*$");
    private static final Pattern P_IMAGE_URL = Pattern.compile("^.*/([\\w-]+\\.jpg).*$");
    private final Options options;
    private CommandLine cli;
    private final List<String> urls = new ArrayList<>();

    private IristaDownloader() {
        options = createOptions();
    }


    public static void main(String[] args) {
        IristaDownloader downloader = new IristaDownloader();
        try {
            downloader.parseArgs(args);
            downloader.downloadImageList();
            downloader.downloadImages();
        } catch (IristaDownloaderException e) {
            downloader.logError(e);
        }
    }

    private void downloadImageList() throws IristaDownloaderException {
        String listPhotoUrl = getListPhotoUrl();
        String rssContents = getText(listPhotoUrl);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode tree = objectMapper.readTree(rssContents);
            JsonNode content = tree.get("content");
            if (content.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = content.fields();
                while (it.hasNext()) {
                    String dlink = it.next().getValue().get("dlink").asText();
                    urls.add(dlink);

                }
            }
        } catch (IOException e) {
            throw new IristaDownloaderException(e);
        }
        System.out.println("" + urls.size() + " URLs captured");
    }

    private void downloadImages()  throws IristaDownloaderException {
        try {
            File destinationFolder = (File)cli.getParsedOptionValue(DESTINATION_FOLDER_OPTION);
            for (String url : urls) {
                Matcher m = P_IMAGE_URL.matcher(url);
                if (m.matches()) {
                    String filename = m.group(1);
                    File destination = new File(destinationFolder, filename);
                    downloadFile(url, destination);
                    System.out.println("Image " + destination + " saved.");
                }
                else {
                    throw new IristaDownloaderException(url + " does not match pattern " + P_IMAGE_URL.pattern());
                }
            }
        }
        catch (ParseException e) {
            throw new IristaDownloaderException(e);
        }
    }


    private void downloadFile(String url, File filename) throws IristaDownloaderException{
        try {
            URL website = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(filename);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        catch(IOException e) {
            throw new IristaDownloaderException(e);
        }
    }


    public static String getText(String url) throws IristaDownloaderException {
        try {
            URL website = new URL(url);
            URLConnection connection = website.openConnection();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream()));

            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            in.close();

            return response.toString();
        } catch (IOException e) {
            throw new IristaDownloaderException(e);
        }
    }

    private String getListPhotoUrl() throws IristaDownloaderException {
        String listPhotoUrl = null;
        try {
            String albumUrl = (String) cli.getParsedOptionValue(ALBUM_OPTION);
            Matcher albumIdMatcher = P_ALBUM_ID.matcher(albumUrl);
            albumIdMatcher.matches();
            String albumId = albumIdMatcher.group(1);
            listPhotoUrl = "https://www.irista.com/lifecake/album/" + albumId;
        } catch (ParseException e) {
            throw new IristaDownloaderException(e);
        }
        return listPhotoUrl;
    }

    private void logError(IristaDownloaderException e) {
        System.err.println(e.getMessage() + ": " + e.getCause().getMessage());
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("java fr.leyrdhin.tools.IristaDownloader", options);
    }

    private void parseArgs(String[] args) throws IristaDownloaderException {
        DefaultParser parser = new DefaultParser();
        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            throw new IristaDownloaderException(e);
        }
    }


    private Options createOptions() {
        return new Options().addOption(Option.builder(ALBUM_OPTION).longOpt("album")
                .desc("shared url of the album on irista.com").hasArg().argName("shared_url").required()
                .type(String.class).build())
                .addOption(Option.builder(DESTINATION_FOLDER_OPTION).longOpt("destination")
                        .desc("folder in which downloaded images will be saved").hasArg().argName("folder")
                .type(File.class).required().build());
    }


}
