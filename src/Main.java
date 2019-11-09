import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringEscapeUtils;

public class Main extends Application {

    public String getHTML(String ytURL) {
        HttpURLConnection conn = null;
        StringBuilder contents = new StringBuilder();
        try {
            conn = (HttpURLConnection)new URL(ytURL).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            InputStream is = conn.getInputStream();

            String enc = conn.getContentEncoding();

            if (enc == null) {
                Pattern p = Pattern.compile("charset=(.*)");
                Matcher m = p.matcher(conn.getHeaderField("Content-Type"));
                if (m.find()) {
                    enc = m.group(1);
                }
            }

            if (enc == null)
                enc = "UTF-8";

            BufferedReader br = new BufferedReader(new InputStreamReader(is, enc));

            String line = null;

            while ((line = br.readLine()) != null) {
                contents.append(line);
                contents.append("\n");
            }
        }
        catch(IOException e){
        }

        return contents.toString();
    }

    public List<String> getVideoList(String html) {
        List<String> urlList = new ArrayList<String>();
        Pattern urlencod = Pattern.compile("\"url_encoded_fmt_stream_map\":\"([^\"]*)\"");
        Matcher urlencodMatch = urlencod.matcher(html);
        if (urlencodMatch.find()) {
            String url_encoded_fmt_stream_map;
            url_encoded_fmt_stream_map = urlencodMatch.group(1);
            Pattern encod = Pattern.compile("url=(.*)");
            Matcher encodMatch = encod.matcher(url_encoded_fmt_stream_map);
            if (encodMatch.find()) {
                String sline = encodMatch.group(1);
                String[] urlStrings = sline.split("url=");
                for (String urlString : urlStrings) {
                    String url = null;
                    urlString = StringEscapeUtils.unescapeJava(urlString);
                    Pattern link = Pattern.compile("([^&,]*)[&,]");
                    Matcher linkMatch = link.matcher(urlString);
                    if (linkMatch.find()) {
                        url = linkMatch.group(1);
                        url = URLDecoder.decode(url, StandardCharsets.UTF_8);
                    }

                    if (url != null && url.contains("&mime=video%2Fmp4&"))
                        urlList.add(url);
                }
            }
        }

        return urlList;
    }

    @Override
    public void start(Stage primaryStage) {
        Group root = new Group();
        Scene scene = new Scene(root,500,400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Playing video");
        primaryStage.show();

        MediaView mediaView = new MediaView();
        root.getChildren().add(mediaView);

        TextField textField = new TextField();
        root.getChildren().add(textField);

        AtomicReference<MediaPlayer> mediaPlayer = new AtomicReference<>();

        Button button = new Button("Load Video");

        button.setOnAction(actionEvent -> {
            String html = getHTML(textField.getText());
            List<String> videos = getVideoList(html);

            Media media = new Media(videos.iterator().next());

            if (mediaPlayer.get() !=  null)
                mediaPlayer.get().dispose();

            mediaPlayer.set(new MediaPlayer(media));

            mediaPlayer.get().setOnReady(() -> {
                int h = media.getHeight();
                int w = media.getWidth();
                System.out.println("Height: " + h + ", Width: " + w);
                primaryStage.setHeight(h);
                primaryStage.setWidth(w);
                mediaPlayer.get().play();
            });

            mediaView.setMediaPlayer(mediaPlayer.get());

        });

        root.getChildren().add(button);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
