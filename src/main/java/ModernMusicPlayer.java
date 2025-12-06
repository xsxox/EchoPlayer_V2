import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ModernMusicPlayer extends Application {

    private MediaPlayer mediaPlayer;
    private List<File> playList = new ArrayList<>();
    private int currentIndex = -1;

    // --- UI 变量 ---
    private BorderPane root;
    private VBox leftPanel;
    private ListView<String> playlistView;
    private Label listTitle;
    private Label titleLabel;
    private Label artistLabel;
    private Label timeLabel;
    private Slider volumeSlider;
    private Slider progressSlider;
    private Button btnPlay;
    private Button btnPrev;
    private Button btnNext;
    private Button btnAdd;
    private Label volIcon;
    private ComboBox<String> themeSelector;

    // --- 黑胶组件 ---
    private StackPane vinylRecord;
    private Circle disc;
    private Circle labelCenter;
    private Text vinylText;
    private javafx.animation.RotateTransition rotateAnimation;

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();

        // --- 左侧 ---
        leftPanel = new VBox(15);
        leftPanel.setPadding(new Insets(25));
        leftPanel.setPrefWidth(260);

        listTitle = new Label("LIBRARY");
        listTitle.setFont(Font.font("Verdana", FontWeight.BOLD, 13));

        playlistView = new ListView<>();
        playlistView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        VBox.setVgrow(playlistView, Priority.ALWAYS);

        playlistView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int idx = playlistView.getSelectionModel().getSelectedIndex();
                if (idx >= 0) playSong(idx);
            }
        });

        btnAdd = new Button("➕ IMPORT TRACKS");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setPrefHeight(40);
        btnAdd.setOnAction(e -> addMusic(primaryStage));

        // 主题选择器
        themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll("🌑 Classic Dark", "⚪️ Apple Clean", "👾 Cyberpunk", "💧 Dynamic Blue");
        themeSelector.setValue("🌑 Classic Dark"); // 默认选中 Classic Dark 测试
        themeSelector.setOnAction(e -> applyTheme(themeSelector.getValue()));
        themeSelector.setMaxWidth(Double.MAX_VALUE);
        themeSelector.setPrefHeight(35);

        VBox bottomLeft = new VBox(15, btnAdd, themeSelector);
        leftPanel.getChildren().addAll(listTitle, playlistView, bottomLeft);
        root.setLeft(leftPanel);

        // --- 中间 ---
        VBox centerPanel = new VBox(30);
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setPadding(new Insets(40));

        // 1. 黑胶
        createVinylRecord();

        // 2. 信息
        VBox infoBox = new VBox(10);
        infoBox.setAlignment(Pos.CENTER);
        titleLabel = new Label("EchoPlayer");
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 28));

        artistLabel = new Label("System Ready");
        artistLabel.setFont(Font.font("Microsoft YaHei", FontWeight.NORMAL, 16));

        infoBox.getChildren().addAll(titleLabel, artistLabel);

        // 3. 进度
        VBox progressBox = new VBox(8);
        progressSlider = new Slider();
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        HBox timeContainer = new HBox(timeLabel);
        timeContainer.setAlignment(Pos.CENTER_RIGHT);
        progressBox.getChildren().addAll(progressSlider, timeContainer);

        // 4. 按钮
        HBox controls = new HBox(35);
        controls.setAlignment(Pos.CENTER);
        btnPrev = createIconButton("⏮");
        btnPlay = createPlayButton();
        btnNext = createIconButton("⏭");

        HBox volBox = new HBox(10);
        volBox.setAlignment(Pos.CENTER);
        volIcon = new Label("🔊");
        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(100);
        volBox.getChildren().addAll(volIcon, volumeSlider);

        HBox bottomBar = new HBox(50);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.getChildren().addAll(controls, volBox);

        controls.getChildren().addAll(btnPrev, btnPlay, btnNext);
        centerPanel.getChildren().addAll(vinylRecord, infoBox, progressBox, bottomBar);
        root.setCenter(centerPanel);

        // --- 绑定 ---
        btnPlay.setOnAction(e -> togglePlay());
        btnPrev.setOnAction(e -> playPrev());
        btnNext.setOnAction(e -> playNextSong());
        setupSliderListeners();

        // --- 启动 ---
        applyTheme("🌑 Classic Dark");

        Scene scene = new Scene(root, 1050, 720);
        primaryStage.setTitle("EchoPlayer V8 - Perfect UI");
        primaryStage.setScene(scene);
        primaryStage.show();

        loadProjectMusic();
        loadSavedPlaylist();
    }

    // ==========================================
    //   🎨 主题核心逻辑 (修复下拉框白底问题)
    // ==========================================
    private void applyTheme(String themeName) {
        String bgRoot, bgLeft, textMain, textSub, accentColor, sliderTrack;
        String comboBg, comboText, comboBorder, comboHover; // 新增 hover 颜色

        boolean isLightMode = false;
        boolean isCyberpunk = false;
        boolean isDynamicBlue = false;

        switch (themeName) {
            case "⚪️ Apple Clean":
                bgRoot = "#FFFFFF";
                bgLeft = "#F5F5F7";
                textMain = "#1D1D1F";
                textSub = "#86868B";
                accentColor = "#FA2D48";
                sliderTrack = "#E5E5E5";

                comboBg = "#FFFFFF";
                comboText = "#000000";
                comboBorder = "#D1D1D6";
                comboHover = "#F2F2F7"; // 浅灰悬停
                isLightMode = true;
                break;

            case "👾 Cyberpunk":
                bgRoot = "#0b0b19";
                bgLeft = "#13132b";
                textMain = "#00f3ff";
                textSub = "#ff0099";
                accentColor = "#00f3ff";
                sliderTrack = "#2a2a40";

                comboBg = "#2a2a40";
                comboText = "#00f3ff";
                comboBorder = "#ff0099";
                comboHover = "#3d3d5c"; // 稍亮的深紫悬停
                isCyberpunk = true;
                break;

            case "💧 Dynamic Blue":
                bgRoot = "#0f172a";
                bgLeft = "#1e293b";
                textMain = "#e0f2fe";
                textSub = "#94a3b8";
                accentColor = "#38bdf8";
                sliderTrack = "#334155";

                comboBg = "#334155";
                comboText = "#ffffff";
                comboBorder = "#38bdf8";
                comboHover = "#475569"; // 亮一点的蓝灰悬停
                isDynamicBlue = true;
                break;

            case "🌑 Classic Dark":
            default:
                bgRoot = "#121212";
                bgLeft = "#000000";
                textMain = "#FFFFFF";
                textSub = "#B3B3B3";
                accentColor = "#1DB954";
                sliderTrack = "#404040";

                comboBg = "#333333";
                comboText = "#FFFFFF";
                comboBorder = "#555555";
                comboHover = "#444444"; // 经典灰悬停
                break;
        }

        // 1. 背景与文字
        root.setStyle("-fx-background-color: " + bgRoot + ";");
        leftPanel.setStyle("-fx-background-color: " + bgLeft + ";");
        listTitle.setTextFill(Color.web(accentColor));
        titleLabel.setTextFill(Color.web(textMain));
        artistLabel.setTextFill(Color.web(textSub));
        timeLabel.setTextFill(Color.web(textSub));
        volIcon.setTextFill(Color.web(textSub));
        titleLabel.setEffect(isCyberpunk ? new Glow(0.8) : null);
        artistLabel.setEffect(isCyberpunk ? new DropShadow(10, Color.web("#ff0099")) : null);

        // 2. 滑块
        String commonSliderStyle = "-fx-control-inner-background: " + sliderTrack + "; -fx-accent: " + accentColor + ";";
        progressSlider.setStyle(commonSliderStyle);
        volumeSlider.setStyle(commonSliderStyle);

        // 3. 列表样式
        final String finalMainText = textMain;
        final String selectionColor = isLightMode ? "rgba(0,0,0,0.05)" : (isDynamicBlue ? "rgba(56, 189, 248, 0.2)" : "rgba(255,255,255,0.1)");
        playlistView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setTextFill(Color.web(finalMainText));
                    String baseStyle = "-fx-font-size: 15px; -fx-padding: 10 5 10 5;";
                    if (isSelected()) {
                        setStyle(baseStyle + "-fx-background-color: " + selectionColor + "; -fx-font-weight: bold;");
                    } else {
                        setStyle(baseStyle + "-fx-background-color: transparent;");
                    }
                }
            }
        });
        playlistView.refresh();

        // 4. ✨✨✨ 下拉框终极修复 ✨✨✨

        // A. 按钮本身样式
        themeSelector.setStyle(
                "-fx-background-color: " + comboBg + "; " +
                        "-fx-font-size: 13px; " +
                        "-fx-border-color: " + comboBorder + "; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5;"
        );

        final String finalComboText = comboText;
        final String finalComboBg = comboBg;
        final String finalComboHover = comboHover;

        // B. 按钮显示区域 (ButtonCell)
        themeSelector.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setTextFill(Color.web(finalComboText));
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        // C. 下拉弹窗列表 (Popup List)
        themeSelector.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    // 关键：空行也要填充背景色，否则会出现白色底
                    setStyle("-fx-background-color: " + finalComboBg + ";");
                } else {
                    setText(item);
                    setTextFill(Color.web(finalComboText));

                    // 默认状态
                    setStyle("-fx-background-color: " + finalComboBg + "; -fx-padding: 5 10 5 10;");

                    // 鼠标悬停变色效果 (模拟 CSS Hover)
                    setOnMouseEntered(e -> {
                        setStyle("-fx-background-color: " + finalComboHover + "; -fx-padding: 5 10 5 10;");
                    });

                    // 鼠标移出恢复默认
                    setOnMouseExited(e -> {
                        setStyle("-fx-background-color: " + finalComboBg + "; -fx-padding: 5 10 5 10;");
                    });
                }
            }
        });

        // 5. 按钮样式
        String playBg = isLightMode ? "#333" : (isDynamicBlue ? "#38bdf8" : (isCyberpunk ? "#00f3ff" : "#FFF"));
        String playFg = (isCyberpunk || isDynamicBlue) ? "#000" : "#FFF";
        if (themeName.equals("🌑 Classic Dark")) playFg = "#000";

        btnPlay.setStyle(
                "-fx-background-color: " + playBg + "; " +
                        "-fx-text-fill: " + playFg + "; " +
                        "-fx-background-radius: 100; " +
                        "-fx-font-size: 22px; " +
                        "-fx-min-width: 55px; " +
                        "-fx-min-height: 55px;"
        );
        btnPlay.setEffect(isDynamicBlue ? new DropShadow(15, Color.web("#38bdf8")) : (isCyberpunk ? new DropShadow(15, Color.web("#00f3ff")) : null));

        updateButtonStyle(btnAdd, textSub, accentColor, isLightMode);
        updateButtonStyle(btnPrev, textMain, accentColor, isLightMode);
        updateButtonStyle(btnNext, textMain, accentColor, isLightMode);

        updateVinylStyle(themeName);
    }

    private void updateButtonStyle(Button btn, String normalColor, String hoverColor, boolean isLight) {
        String borderColor = (btn == btnAdd) ? (isLight ? "#CCC" : "#555") : "transparent";
        String hoverBorder = (btn == btnAdd) ? hoverColor : "transparent";
        String size = (btn == btnAdd) ? "13" : "22";

        String base = "-fx-background-color: transparent; -fx-text-fill: " + normalColor + "; -fx-font-size: " + size + "px; -fx-border-color: " + borderColor + "; -fx-border-radius: 20;";
        String hover = "-fx-background-color: transparent; -fx-text-fill: " + hoverColor + "; -fx-font-size: " + size + "px; -fx-border-color: " + hoverBorder + "; -fx-border-radius: 20;";

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private void updateVinylStyle(String theme) {
        disc.setFill(Color.BLACK);
        vinylText.setFill(Color.WHITE);

        if (theme.equals("⚪️ Apple Clean")) {
            disc.setFill(Color.web("#333"));
            disc.setEffect(new DropShadow(10, Color.web("#999")));
            labelCenter.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE, new Stop(0, Color.web("#FF99AA")), new Stop(1, Color.web("#FA2D48"))));

        } else if (theme.equals("👾 Cyberpunk")) {
            disc.setFill(Color.BLACK);
            disc.setEffect(new DropShadow(20, Color.web("#00f3ff")));
            vinylText.setFill(Color.web("#00f3ff"));
            labelCenter.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE, new Stop(0, Color.web("#00f3ff")), new Stop(1, Color.web("#ff0099"))));

        } else if (theme.equals("💧 Dynamic Blue")) {
            disc.setFill(Color.web("#020617"));
            disc.setEffect(new DropShadow(20, Color.web("#38bdf8")));
            vinylText.setFill(Color.web("#e0f2fe"));
            labelCenter.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE, new Stop(0, Color.web("#7dd3fc")), new Stop(1, Color.web("#0ea5e9"))));

        } else { // Classic
            disc.setEffect(new DropShadow(15, Color.BLACK));
            labelCenter.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE, new Stop(0, Color.web("#444")), new Stop(1, Color.web("#1DB954"))));
        }
    }

    // ==========================================
    //   核心黑胶创建
    // ==========================================
    private void createVinylRecord() {
        vinylRecord = new StackPane();
        vinylRecord.setMaxSize(280, 280);
        vinylRecord.setMinSize(280, 280);

        disc = new Circle(140);

        Circle groove1 = new Circle(125);
        groove1.setFill(Color.TRANSPARENT);
        groove1.setStroke(Color.rgb(255,255,255,0.1));
        groove1.setStrokeWidth(2);

        Circle groove2 = new Circle(105);
        groove2.setFill(Color.TRANSPARENT);
        groove2.setStroke(Color.rgb(255,255,255,0.1));
        groove2.setStrokeWidth(2);

        Rectangle shine = new Rectangle(20, 260);
        shine.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.5, Color.rgb(255, 255, 255, 0.1)),
                new Stop(1, Color.TRANSPARENT)));
        shine.setRotate(45);

        labelCenter = new Circle(50);
        vinylText = new Text("ECHO");
        vinylText.setFont(Font.font("Verdana", FontWeight.BOLD, 16));

        Circle hole = new Circle(6, Color.BLACK);

        vinylRecord.getChildren().addAll(disc, groove1, groove2, shine, labelCenter, vinylText, hole);

        rotateAnimation = new RotateTransition(Duration.seconds(6), vinylRecord);
        rotateAnimation.setByAngle(360);
        rotateAnimation.setCycleCount(RotateTransition.INDEFINITE);
        rotateAnimation.setInterpolator(Interpolator.LINEAR);
    }

    // --- 播放逻辑 ---
    private void togglePlay() {
        if (mediaPlayer == null && !playList.isEmpty()) {
            int idx = playlistView.getSelectionModel().getSelectedIndex();
            playSong(idx >= 0 ? idx : 0);
        } else if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                rotateAnimation.pause();
                btnPlay.setText("▶");
            } else {
                mediaPlayer.play();
                rotateAnimation.play();
                btnPlay.setText("⏸");
            }
        }
    }

    private void playSong(int index) {
        if (index < 0 || index >= playList.size()) return;
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }

        currentIndex = index;
        File file = playList.get(index);

        playlistView.getSelectionModel().select(index);
        titleLabel.setText(file.getName().replace(".mp3", ""));
        artistLabel.setText("Now Playing");
        btnPlay.setText("⏸");

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volumeSlider.getValue());
            mediaPlayer.play();

            vinylRecord.setRotate(0);
            rotateAnimation.playFromStart();

            mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
                if (!progressSlider.isValueChanging()) {
                    progressSlider.setValue((newT.toMillis() / media.getDuration().toMillis()) * 100);
                }
                timeLabel.setText(formatTime(newT) + " / " + formatTime(media.getDuration()));
            });

            mediaPlayer.setOnEndOfMedia(this::playNextSong);

        } catch (Exception e) {
            artistLabel.setText("Load Error");
        }
    }

    private void playNextSong() {
        if (playList.isEmpty()) return;
        int newIndex = currentIndex + 1;
        if (newIndex >= playList.size()) newIndex = 0;
        playSong(newIndex);
    }

    private void playPrev() {
        if (playList.isEmpty()) return;
        int newIndex = currentIndex - 1;
        if (newIndex < 0) newIndex = playList.size() - 1;
        playSong(newIndex);
    }

    // --- 数据加载 ---
    private void addMusic(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav"));
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null) { for (File f : files) addToPlaylistSafe(f); }
    }

    private void loadProjectMusic() {
        File folder = new File("music");
        if (!folder.exists()) { folder.mkdir(); return; }
        File[] files = folder.listFiles((d, n) -> n.toLowerCase().endsWith(".mp3") || n.toLowerCase().endsWith(".wav"));
        if (files != null) { for (File f : files) addToPlaylistSafe(f); }
    }

    private void loadSavedPlaylist() {
        try {
            File f = new File("playlist.txt");
            if (f.exists()) {
                List<String> lines = Files.readAllLines(Paths.get(f.toURI()));
                for (String path : lines) {
                    File file = new File(path);
                    if (file.exists()) addToPlaylistSafe(file);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addToPlaylistSafe(File file) {
        if (playList.stream().noneMatch(f -> f.getName().equals(file.getName()))) {
            playList.add(file);
            playlistView.getItems().add(file.getName());
        }
    }

    @Override public void stop() throws Exception {
        super.stop();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("playlist.txt"))) {
            for (File f : playList) { writer.write(f.getAbsolutePath()); writer.newLine(); }
        }
    }

    private Button createIconButton(String icon) {
        Button btn = new Button(icon);
        return btn;
    }

    private Button createPlayButton() {
        Button btn = new Button("▶");
        return btn;
    }

    private void setupSliderListeners() {
        volumeSlider.valueProperty().addListener((o, ov, nv) -> { if (mediaPlayer != null) mediaPlayer.setVolume(nv.doubleValue()); });
        progressSlider.valueProperty().addListener((o, ov, nv) -> { if (progressSlider.isValueChanging() && mediaPlayer != null) mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(nv.doubleValue() / 100.0)); });
        progressSlider.setOnMouseClicked(e -> { if (mediaPlayer != null) mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(e.getX() / progressSlider.getWidth())); });
    }

    private String formatTime(Duration d) {
        if (d == null) return "00:00";
        int s = (int) d.toSeconds();
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    public static void main(String[] args) { launch(args); }
}
