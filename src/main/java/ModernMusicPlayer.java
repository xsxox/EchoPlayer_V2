import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Reflection;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

public class ModernMusicPlayer extends Application {

    private MediaPlayer mediaPlayer;
    private List<File> playList = new ArrayList<>();
    private ObservableList<String> listModel = FXCollections.observableArrayList();
    private FilteredList<String> filteredList;
    private int currentIndex = -1;

    private enum PlayMode { LOOP_ALL, SHUFFLE, LOOP_ONE }
    private PlayMode currentMode = PlayMode.LOOP_ALL;

    // --- SVG 图标数据 (细线条版) ---
    private static final String SVG_SHUFFLE = "M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z";
    private static final String SVG_LOOP = "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z";
    private static final String SVG_ONE = "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3zm-1-9h-1v4h2V9z";
    private static final String SVG_PREV = "M6 6h2v12H6zm3.5 6l8.5 6V6z";
    private static final String SVG_NEXT = "M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z";
    private static final String SVG_SEARCH = "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z";

    // --- UI 变量 ---
    private BorderPane root;
    private VBox leftPanel;
    private ListView<String> playlistView;
    private TextField searchField;
    private Label listTitle;
    private Label titleLabel;
    private Label artistLabel;
    private Label timeLabel;
    private Slider volumeSlider;
    private Slider progressSlider;
    private Button btnPlay, btnPrev, btnNext, btnMode, btnAdd;
    private Label volIcon;
    private ComboBox<String> themeSelector;

    // --- 核心显示区 (切换黑胶/倒影频谱) ---
    private StackPane centerDisplayArea;
    private HBox visualizerBox;
    private Rectangle[] spectrumBars;
    private static final int BANDS = 50;

    private String currentPlayBtnStyleBase = "";
    private String currentAccentColor = "#1DB954";

    // --- 黑胶组件 ---
    private StackPane vinylRecord;
    private Circle disc, labelCenter;
    private Text vinylText;
    private RotateTransition rotateAnimation;

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();

        // --- 左侧 ---
        leftPanel = new VBox(15);
        leftPanel.setPadding(new Insets(25));
        leftPanel.setPrefWidth(280);

        listTitle = new Label("LIBRARY");
        listTitle.setFont(Font.font("Verdana", FontWeight.BOLD, 13));

        HBox searchBox = createSearchBox();

        filteredList = new FilteredList<>(listModel, p -> true);
        playlistView = new ListView<>(filteredList);
        playlistView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        VBox.setVgrow(playlistView, Priority.ALWAYS);

        playlistView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                String selectedItem = playlistView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) playSongByName(selectedItem);
            }
        });

        btnAdd = new Button("➕ IMPORT TRACKS");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setPrefHeight(45);
        btnAdd.setOnAction(e -> addMusic(primaryStage));

        themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll("Spotify Dark", "Apple Clean", "Cyberpunk", "Dynamic Blue");
        themeSelector.setValue("Spotify Dark");
        themeSelector.setOnAction(e -> applyTheme(themeSelector.getValue()));
        themeSelector.setMaxWidth(Double.MAX_VALUE);
        themeSelector.setPrefHeight(35);

        VBox bottomLeft = new VBox(15, btnAdd, themeSelector);
        leftPanel.getChildren().addAll(listTitle, searchBox, playlistView, bottomLeft);
        root.setLeft(leftPanel);

        // --- 中间 ---
        VBox centerPanel = new VBox(25);
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setPadding(new Insets(30, 40, 30, 40));

        // 1. 创建显示区域 (可切换)
        centerDisplayArea = new StackPane();
        centerDisplayArea.setMaxSize(280, 280);
        centerDisplayArea.setMinSize(280, 280);
        centerDisplayArea.setStyle("-fx-cursor: hand;");
        centerDisplayArea.setOnMouseClicked(e -> toggleMainView());

        // 创建黑胶
        createVinylRecord();

        // 创建倒影频谱 (默认隐藏)
        createSkylineVisualizer();
        visualizerBox.setVisible(false);
        visualizerBox.setOpacity(0);

        centerDisplayArea.getChildren().addAll(vinylRecord, visualizerBox);

        // 2. 信息
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER);
        titleLabel = new Label("EchoPlayer");
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 28));
        artistLabel = new Label("Click Disc to Switch Mode");
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
        btnMode = createSvgButton(SVG_LOOP); updateModeButtonText(); btnMode.setOnAction(e -> togglePlayMode());
        btnPrev = createSvgButton(SVG_PREV);
        btnPlay = createPlayButton();
        btnNext = createSvgButton(SVG_NEXT);
        controls.getChildren().addAll(btnMode, btnPrev, btnPlay, btnNext);

        HBox volBox = new HBox(10);
        volBox.setAlignment(Pos.CENTER);
        volIcon = new Label("🔊");
        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(100);
        volBox.getChildren().addAll(volIcon, volumeSlider);

        HBox bottomBar = new HBox(50);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.getChildren().addAll(controls, volBox);

        centerPanel.getChildren().addAll(centerDisplayArea, infoBox, progressBox, bottomBar);
        root.setCenter(centerPanel);

        // --- 绑定 ---
        btnPlay.setOnAction(e -> togglePlay());
        btnPrev.setOnAction(e -> playPrev());
        btnNext.setOnAction(e -> playNextSong());
        setupSliderListeners();

        Scene scene = new Scene(root, 1050, 750);

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) togglePlay();
            else if (event.getCode() == KeyCode.LEFT) playPrev();
            else if (event.getCode() == KeyCode.RIGHT) playNextSong();
        });

        setupDragAndDrop(scene);
        hideScrollBars(scene);

        primaryStage.setTitle("EchoPlayer V19 - Ultimate Stable Edition");
        primaryStage.setScene(scene);
        primaryStage.show();

        applyTheme("Spotify Dark");

        loadProjectMusic();
        loadSavedPlaylist();
    }

    // ==========================================
    //   核心功能：切换视图
    // ==========================================
    private void toggleMainView() {
        boolean showVisualizer = vinylRecord.isVisible();
        if (showVisualizer) {
            FadeTransition ft1 = new FadeTransition(Duration.millis(300), vinylRecord);
            ft1.setFromValue(1.0); ft1.setToValue(0.0);
            ft1.setOnFinished(e -> vinylRecord.setVisible(false));
            ft1.play();
            visualizerBox.setVisible(true);
            FadeTransition ft2 = new FadeTransition(Duration.millis(300), visualizerBox);
            ft2.setFromValue(0.0); ft2.setToValue(1.0);
            ft2.play();
        } else {
            FadeTransition ft1 = new FadeTransition(Duration.millis(300), visualizerBox);
            ft1.setFromValue(1.0); ft1.setToValue(0.0);
            ft1.setOnFinished(e -> visualizerBox.setVisible(false));
            ft1.play();
            vinylRecord.setVisible(true);
            FadeTransition ft2 = new FadeTransition(Duration.millis(300), vinylRecord);
            ft2.setFromValue(0.0); ft2.setToValue(1.0);
            ft2.play();
        }
    }

    // ==========================================
    //   Skyline 倒影频谱
    // ==========================================
    private void createSkylineVisualizer() {
        visualizerBox = new HBox(3);
        visualizerBox.setAlignment(Pos.BOTTOM_CENTER);
        visualizerBox.setMaxSize(280, 280);
        visualizerBox.setMinSize(280, 280);
        visualizerBox.setPadding(new Insets(0, 0, 40, 0));

        Reflection reflection = new Reflection();
        reflection.setFraction(0.4);
        reflection.setTopOpacity(0.3);
        reflection.setBottomOpacity(0.0);
        visualizerBox.setEffect(reflection);

        spectrumBars = new Rectangle[BANDS];
        for (int i = 0; i < BANDS; i++) {
            Rectangle bar = new Rectangle(4, 5);
            bar.setArcWidth(4);
            bar.setArcHeight(4);
            bar.setFill(Color.web(currentAccentColor));
            spectrumBars[i] = bar;
            visualizerBox.getChildren().add(bar);
        }
    }

    private void updateVisualizerColor(String color) {
        currentAccentColor = color;
        if (spectrumBars != null) {
            for (Rectangle bar : spectrumBars) bar.setFill(Color.web(color));
        }
    }

    // ==========================================
    //   搜索框
    // ==========================================
    private HBox createSearchBox() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        SVGPath icon = new SVGPath();
        icon.setContent(SVG_SEARCH);
        icon.setScaleX(0.8); icon.setScaleY(0.8);
        icon.setFill(Color.web("#888"));

        searchField = new TextField();
        searchField.setPromptText("Search library...");
        searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: #666;");
        searchField.setPrefWidth(200);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(songName -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return songName.toLowerCase().contains(newValue.toLowerCase());
            });
        });

        box.getChildren().addAll(icon, searchField);
        box.setStyle("-fx-border-color: transparent transparent #444 transparent; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 5 0;");
        return box;
    }

    // ==========================================
    //   播放逻辑
    // ==========================================
    private void playSongByName(String name) {
        for (int i = 0; i < playList.size(); i++) {
            if (playList.get(i).getName().equals(name)) {
                playSong(i);
                return;
            }
        }
    }

    private void playSong(int index) {
        // 1. 越界检查
        if (index < 0 || index >= playList.size()) return;

        // 2. 停止上一首
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        // 3. 更新当前索引和文件
        currentIndex = index;
        File file = playList.get(index);

        // --- 🔥 修复核心：同步左侧列表的高亮选中状态 ---
        // 获取当前文件名
        String songName = file.getName();
        // 在左侧列表视图中查找这个名字的位置
        // (这样做是为了兼容搜索状态，搜索时列表顺序可能变了，直接用index不对)
        int listIndex = playlistView.getItems().indexOf(songName);

        if (listIndex != -1) {
            // 选中该行
            playlistView.getSelectionModel().select(listIndex);
            // 自动滚动到该行 (防止切歌时歌曲在屏幕外面看不见)
            playlistView.scrollTo(listIndex);
        }
        // ---------------------------------------------

        // 4. 更新界面文字
        titleLabel.setText(file.getName().replace(".mp3", ""));
        artistLabel.setText("Now Playing");
        updatePlayButtonIconStyle(true);

        // 5. 创建播放器并开始播放
        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volumeSlider.getValue());

            // 频谱可视化监听
            mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
                for (int i = 0; i < BANDS && i < magnitudes.length; i++) {
                    double mag = magnitudes[i] + 60;
                    if (mag < 0) mag = 0;
                    double height = mag * 3.0 + 5;
                    if(height > 220) height = 220;
                    spectrumBars[i].setHeight(height);
                }
            });
            mediaPlayer.setAudioSpectrumInterval(0.04);

            mediaPlayer.play();

            // 重置动画
            vinylRecord.setRotate(0);
            rotateAnimation.playFromStart();

            // 进度条监听
            mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
                if (!progressSlider.isValueChanging()) {
                    progressSlider.setValue((newT.toMillis() / media.getDuration().toMillis()) * 100);
                }
                timeLabel.setText(formatTime(newT) + " / " + formatTime(media.getDuration()));
            });

            // 播放结束自动下一首
            mediaPlayer.setOnEndOfMedia(this::playNextSong);

        } catch (Exception e) {
            artistLabel.setText("Load Error");
            e.printStackTrace(); // 方便调试
        }
    }

    // ==========================================
    //   右键菜单 & CSS 注入
    // ==========================================
    private ContextMenu createContextMenu(String item) {
        ContextMenu cm = new ContextMenu();
        MenuItem playItem = new MenuItem("▶ Play");
        playItem.setOnAction(e -> playSongByName(item));

        MenuItem openItem = new MenuItem("📂 Open File Location");
        openItem.setOnAction(e -> {
            for (File f : playList) {
                if (f.getName().equals(item)) {
                    try { Desktop.getDesktop().open(f.getParentFile()); } catch (Exception ex) { ex.printStackTrace(); }
                    break;
                }
            }
        });

        MenuItem deleteItem = new MenuItem("🗑 Remove from Library");
        deleteItem.setOnAction(e -> {
            listModel.remove(item);
            playList.removeIf(f -> f.getName().equals(item));
            if (titleLabel.getText().equals(item.replace(".mp3", ""))) {
                if (mediaPlayer != null) mediaPlayer.stop();
                titleLabel.setText("EchoPlayer"); artistLabel.setText("Stopped");
                updatePlayButtonIconStyle(false); rotateAnimation.stop();
            }
        });

        cm.getItems().addAll(playItem, openItem, new SeparatorMenuItem(), deleteItem);
        return cm;
    }

    private void updateThemeCss(Scene scene, String menuBg, String menuText, String menuHover, String menuBorder) {
        if (scene == null) return;
        String css =
                ".context-menu { -fx-background-color: " + menuBg + "; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + menuBorder + "; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 4); }" +
                        ".menu-item .label { -fx-text-fill: " + menuText + "; }" +
                        ".menu-item:focused { -fx-background-color: " + menuHover + "; }" +
                        ".menu-item:focused .label { -fx-text-fill: " + menuText + "; }";
        try { scene.getStylesheets().add("data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes("UTF-8"))); } catch (Exception e) { e.printStackTrace(); }
    }

    // ==========================================
    //   UI 样式与主题
    // ==========================================
    private void applyTheme(String themeName) {
        String bgRoot, bgLeft, textMain, textSub, accentColor, sliderTrack;
        String comboBg, comboText, comboBorder, comboHover;
        String menuBg, menuText, menuHover, menuBorder;
        String playBtnStyle;

        boolean isLightMode = false;
        boolean isCyberpunk = false;
        boolean isDynamicBlue = false;

        switch (themeName) {
            case "Apple Clean":
                bgRoot = "linear-gradient(to bottom right, #FFFFFF, #F2F2F7)";
                bgLeft = "rgba(245, 245, 247, 0.8)";
                textMain = "#1C1C1E"; textSub = "#8E8E93"; accentColor = "#FA2D48";
                sliderTrack = "#E5E5EA"; comboBg = "#FFFFFF"; comboText = "#1C1C1E"; comboBorder = "#D1D1D6"; comboHover = "#F2F2F7";
                menuBg = "#FFFFFF"; menuText = "#000000"; menuHover = "#F2F2F7"; menuBorder = "#D1D1D6";
                playBtnStyle = "-fx-background-color: linear-gradient(to bottom right, #FF2D55, #FF5E3A); -fx-text-fill: white;";
                isLightMode = true; break;

            case "Spotify Dark":
                bgRoot = "linear-gradient(to bottom, #121212, #181818)";
                bgLeft = "#000000";
                textMain = "#FFFFFF"; textSub = "#B3B3B3"; accentColor = "#1DB954";
                sliderTrack = "#404040"; comboBg = "#282828"; comboText = "#FFFFFF"; comboBorder = "#404040"; comboHover = "#3E3E3E";
                menuBg = "#282828"; menuText = "#FFFFFF"; menuHover = "#404040"; menuBorder = "#333333";
                playBtnStyle = "-fx-background-color: #FFFFFF; -fx-text-fill: #000000;";
                break;

            case "Cyberpunk":
                bgRoot = "linear-gradient(to bottom right, #0b0b19, #1a1a3d)";
                bgLeft = "#13132b";
                textMain = "#00f3ff"; textSub = "#ff0099"; accentColor = "#00f3ff";
                sliderTrack = "#2a2a40"; comboBg = "#2a2a40"; comboText = "#00f3ff"; comboBorder = "#ff0099"; comboHover = "#3d3d5c";
                menuBg = "#1a1a3d"; menuText = "#00f3ff"; menuHover = "#ff0099"; menuBorder = "#00f3ff";
                playBtnStyle = "-fx-background-color: #00f3ff; -fx-text-fill: #000000;";
                isCyberpunk = true; break;

            case "Dynamic Blue":
            default:
                bgRoot = "linear-gradient(to bottom, #0f172a, #1e293b)";
                bgLeft = "#0f172a";
                textMain = "#e0f2fe"; textSub = "#94a3b8"; accentColor = "#38bdf8";
                sliderTrack = "#334155"; comboBg = "#1e293b"; comboText = "#ffffff"; comboBorder = "#38bdf8"; comboHover = "#334155";
                menuBg = "#1e293b"; menuText = "#e0f2fe"; menuHover = "#334155"; menuBorder = "#38bdf8";
                playBtnStyle = "-fx-background-color: #38bdf8; -fx-text-fill: #000000;";
                isDynamicBlue = true; break;
        }

        root.setStyle("-fx-background-color: " + bgRoot + ";");
        leftPanel.setStyle("-fx-background-color: " + bgLeft + "; -fx-border-color: transparent;");
        listTitle.setTextFill(Color.web(accentColor));
        titleLabel.setTextFill(Color.web(textMain));
        artistLabel.setTextFill(Color.web(textSub));
        timeLabel.setTextFill(Color.web(textSub));
        volIcon.setTextFill(Color.web(textSub));

        if (root.getScene() != null) updateThemeCss(root.getScene(), menuBg, menuText, menuHover, menuBorder);

        updateVisualizerColor(accentColor);
        if (searchField != null) {
            String promptColor = isLightMode ? "#999" : "#666";
            searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + textMain + "; -fx-prompt-text-fill: " + promptColor + ";");
        }

        if (isCyberpunk) {
            titleLabel.setEffect(new Glow(0.8));
            artistLabel.setEffect(new DropShadow(10, Color.web("#ff0099")));
        } else {
            titleLabel.setEffect(null);
            artistLabel.setEffect(null);
        }

        String sliderStyle = String.format("-fx-control-inner-background: %s; -fx-accent: %s; -fx-background-color: transparent;", sliderTrack, accentColor);
        progressSlider.setStyle(sliderStyle);
        volumeSlider.setStyle(sliderStyle);

        final String finalMainText = textMain;
        final String hoverColor = isLightMode ? "rgba(0,0,0,0.05)" : "rgba(255,255,255,0.08)";
        final String selectionColor = isLightMode ? "#E5E5EA" : (isCyberpunk ? "rgba(0, 243, 255, 0.2)" : "#333333");

        playlistView.setCellFactory(lv -> new ListCell<String>() {
            private final Text text1 = new Text();
            private final Text text2 = new Text();
            private final Pane container = new Pane(text1, text2);
            private final Rectangle clip = new Rectangle();
            private ParallelTransition marqueeAnimation;
            private final double GAP = 60;

            {
                Font font = Font.font(16);
                text1.setFont(font); text2.setFont(font);
                text1.setTextOrigin(javafx.geometry.VPos.CENTER); text2.setTextOrigin(javafx.geometry.VPos.CENTER);
                container.prefWidthProperty().bind(lv.widthProperty().subtract(40));
                container.setPrefHeight(30);
                clip.widthProperty().bind(container.widthProperty()); clip.heightProperty().bind(container.heightProperty());
                container.setClip(clip);
                text1.layoutYProperty().bind(container.heightProperty().divide(2)); text2.layoutYProperty().bind(container.heightProperty().divide(2));
                text2.setVisible(false);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (marqueeAnimation != null) marqueeAnimation.stop();
                text1.setTranslateX(0); text2.setTranslateX(0); text2.setVisible(false);

                if (empty || item == null) {
                    setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;");
                    setContextMenu(null);
                } else {
                    setText(null); text1.setText(item); text2.setText(item);
                    text1.setFill(Color.web(finalMainText)); text2.setFill(Color.web(finalMainText));
                    setGraphic(container);
                    setContextMenu(createContextMenu(item));

                    String baseStyle = "-fx-padding: 8 15 8 15; -fx-background-radius: 8;";
                    if (isSelected()) {
                        setStyle(baseStyle + "-fx-background-color: " + selectionColor + "; -fx-font-weight: bold;");
                        Platform.runLater(this::startMarquee);
                    } else {
                        setStyle(baseStyle + "-fx-background-color: transparent;");
                    }
                    setOnMouseEntered(e -> { if (!isSelected()) setStyle(baseStyle + "-fx-background-color: " + hoverColor + ";"); });
                    setOnMouseExited(e -> { if (!isSelected()) setStyle(baseStyle + "-fx-background-color: transparent;"); });
                }
            }

            private void startMarquee() {
                if (getItem() == null || !isSelected()) return;
                double textW = text1.getLayoutBounds().getWidth();
                double cellW = container.getWidth();
                if (cellW == 0) cellW = container.getPrefWidth();
                if (textW > cellW && cellW > 0) {
                    text2.setVisible(true);
                    double cycleDistance = textW + GAP;
                    TranslateTransition tt1 = new TranslateTransition(); tt1.setNode(text1); tt1.setFromX(0); tt1.setToX(-cycleDistance); tt1.setInterpolator(Interpolator.LINEAR);
                    TranslateTransition tt2 = new TranslateTransition(); tt2.setNode(text2); tt2.setFromX(cycleDistance); tt2.setToX(0); tt2.setInterpolator(Interpolator.LINEAR);
                    marqueeAnimation = new ParallelTransition(tt1, tt2);
                    double durationSeconds = cycleDistance / 25.0;
                    marqueeAnimation.setCycleCount(Animation.INDEFINITE);
                    tt1.setDuration(Duration.seconds(durationSeconds)); tt2.setDuration(Duration.seconds(durationSeconds));
                    marqueeAnimation.play();
                }
            }
        });
        playlistView.refresh();

        themeSelector.setStyle("-fx-background-color: " + comboBg + "; -fx-text-fill: " + comboText + "; -fx-border-color: " + comboBorder + "; -fx-background-radius: 6; -fx-border-radius: 6;");
        final String fComboBg = comboBg; final String fComboText = comboText; final String fComboHover = comboHover;
        themeSelector.setButtonCell(new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) { setText(item); setTextFill(Color.web(fComboText)); setStyle("-fx-background-color: transparent;"); }
            }
        });
        themeSelector.setCellFactory(lv -> new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle("-fx-background-color: " + fComboBg + ";"); }
                else {
                    setText(item); setTextFill(Color.web(fComboText));
                    setStyle("-fx-background-color: " + fComboBg + "; -fx-padding: 8 10 8 10;");
                    setOnMouseEntered(e -> setStyle("-fx-background-color: " + fComboHover + "; -fx-padding: 8 10 8 10;"));
                    setOnMouseExited(e -> setStyle("-fx-background-color: " + fComboBg + "; -fx-padding: 8 10 8 10;"));
                }
            }
        });

        currentPlayBtnStyleBase = playBtnStyle + "-fx-background-radius: 100; -fx-min-width: 65px; -fx-min-height: 65px; -fx-cursor: hand;";
        boolean isPlaying = (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING);
        updatePlayButtonIconStyle(isPlaying);

        if(themeName.equals("Apple Clean")) btnPlay.setEffect(new DropShadow(15, Color.rgb(255, 45, 85, 0.4)));
        else if (themeName.equals("Spotify Dark")) btnPlay.setEffect(new DropShadow(10, Color.rgb(255, 255, 255, 0.2)));
        else btnPlay.setEffect(isCyberpunk ? new DropShadow(15, Color.web("#00f3ff")) : null);

        addHoverAnimation(btnPlay);
        updateButtonStyle(btnAdd, textSub, accentColor, isLightMode);
        updateButtonStyle(btnPrev, textMain, accentColor, isLightMode);
        updateButtonStyle(btnNext, textMain, accentColor, isLightMode);
        updateButtonStyle(btnMode, textMain, accentColor, isLightMode);
        updateVinylStyle(themeName);
    }

    private Button createSvgButton(String svgContent) {
        Button btn = new Button();
        SVGPath svg = new SVGPath();
        svg.setContent(svgContent);
        svg.fillProperty().bind(btn.textFillProperty());
        svg.setScaleX(0.9); svg.setScaleY(0.9);
        btn.setGraphic(svg);
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        return btn;
    }

    private void updatePlayButtonIconStyle(boolean isPlaying) {
        if (isPlaying) { btnPlay.setStyle(currentPlayBtnStyleBase + "-fx-font-size: 32px; -fx-padding: 0;"); btnPlay.setText("⏸"); }
        else { btnPlay.setStyle(currentPlayBtnStyleBase + "-fx-font-size: 36px; -fx-padding: 0 0 0 4;"); btnPlay.setText("▶"); }
    }

    private void updateButtonStyle(Button btn, String normalColor, String hoverColor, boolean isLight) {
        String borderColor = (btn == btnAdd) ? (isLight ? "#C7C7CC" : "#3A3A3C") : "transparent";
        String hoverBorder = (btn == btnAdd) ? hoverColor : "transparent";
        String size = (btn == btnAdd) ? "12" : "24";
        String fontWeight = (btn == btnAdd) ? "bold" : "normal";
        String base = "-fx-background-color: transparent; -fx-text-fill: " + normalColor + "; -fx-font-size: " + size + "px; -fx-font-weight: " + fontWeight + "; -fx-border-color: " + borderColor + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        String hover = "-fx-background-color: transparent; -fx-text-fill: " + hoverColor + "; -fx-font-size: " + size + "px; -fx-font-weight: " + fontWeight + "; -fx-border-color: " + hoverBorder + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> { btn.setStyle(hover); btn.setScaleX(1.1); btn.setScaleY(1.1); });
        btn.setOnMouseExited(e -> { btn.setStyle(base); btn.setScaleX(1.0); btn.setScaleY(1.0); });
    }

    private void addHoverAnimation(Button btn) {
        btn.setOnMouseEntered(e -> { btn.setScaleX(1.1); btn.setScaleY(1.1); });
        btn.setOnMouseExited(e -> { btn.setScaleX(1.0); btn.setScaleY(1.0); });
    }

    private void updateVinylStyle(String theme) {
        disc.setFill(Color.BLACK);
        vinylText.setFill(Color.WHITE);
        if (theme.equals("Apple Clean")) {
            disc.setFill(Color.web("#2C2C2E")); disc.setEffect(new DropShadow(20, Color.rgb(0,0,0,0.15))); vinylText.setFill(Color.web("#E5E5EA"));
            labelCenter.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE, new Stop(0, Color.web("#FF5E3A")), new Stop(1, Color.web("#FF2D55"))));
        } else if (theme.equals("Spotify Dark")) {
            disc.setFill(Color.web("#121212")); disc.setEffect(new DropShadow(15, Color.rgb(255,255,255,0.05))); vinylText.setFill(Color.web("#AAAAAA"));
            labelCenter.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE, new Stop(0, Color.web("#1DB954")), new Stop(1, Color.web("#191414"))));
        } else if (theme.equals("Cyberpunk")) {
            disc.setFill(Color.BLACK); disc.setEffect(new DropShadow(20, Color.web("#00f3ff"))); vinylText.setFill(Color.web("#00f3ff"));
            labelCenter.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE, new Stop(0, Color.web("#00f3ff")), new Stop(1, Color.web("#ff0099"))));
        } else if (theme.equals("Dynamic Blue")) {
            disc.setFill(Color.web("#020617")); disc.setEffect(new DropShadow(20, Color.web("#38bdf8"))); vinylText.setFill(Color.web("#e0f2fe"));
            labelCenter.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE, new Stop(0, Color.web("#7dd3fc")), new Stop(1, Color.web("#0ea5e9"))));
        }
    }

    private void createVinylRecord() {
        vinylRecord = new StackPane();
        vinylRecord.setMaxSize(280, 280); vinylRecord.setMinSize(280, 280);
        disc = new Circle(140);
        Circle groove1 = new Circle(125); groove1.setFill(Color.TRANSPARENT); groove1.setStroke(Color.rgb(255,255,255,0.1)); groove1.setStrokeWidth(2);
        Circle groove2 = new Circle(105); groove2.setFill(Color.TRANSPARENT); groove2.setStroke(Color.rgb(255,255,255,0.1)); groove2.setStrokeWidth(2);
        Rectangle shine = new Rectangle(20, 260);
        shine.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.TRANSPARENT), new Stop(0.5, Color.rgb(255, 255, 255, 0.1)), new Stop(1, Color.TRANSPARENT)));
        shine.setRotate(45);
        labelCenter = new Circle(50);
        vinylText = new Text("ECHO");
        vinylText.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        Circle hole = new Circle(6, Color.BLACK);
        vinylRecord.getChildren().addAll(disc, groove1, groove2, shine, labelCenter, vinylText, hole);
        rotateAnimation = new RotateTransition(Duration.seconds(6), vinylRecord);
        rotateAnimation.setByAngle(360); rotateAnimation.setCycleCount(RotateTransition.INDEFINITE); rotateAnimation.setInterpolator(Interpolator.LINEAR);
    }

    private void togglePlay() {
        if (mediaPlayer == null && !playList.isEmpty()) {
            if (filteredList.isEmpty()) return;
            playSongByName(filteredList.get(0));
        } else if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause(); rotateAnimation.pause();
                updatePlayButtonIconStyle(false);
            } else {
                mediaPlayer.play(); rotateAnimation.play();
                updatePlayButtonIconStyle(true);
            }
        }
    }

    private void togglePlayMode() {
        switch (currentMode) {
            case LOOP_ALL: currentMode = PlayMode.SHUFFLE; break;
            case SHUFFLE: currentMode = PlayMode.LOOP_ONE; break;
            case LOOP_ONE: currentMode = PlayMode.LOOP_ALL; break;
        }
        updateModeButtonText();
    }

    private void updateModeButtonText() {
        String svgData = "";
        String tooltipText = "";
        switch (currentMode) {
            case LOOP_ALL: svgData = SVG_LOOP; tooltipText = "Loop All"; break;
            case SHUFFLE:  svgData = SVG_SHUFFLE; tooltipText = "Shuffle"; break;
            case LOOP_ONE: svgData = SVG_ONE; tooltipText = "Loop One"; break;
        }
        if (btnMode.getGraphic() instanceof SVGPath) ((SVGPath) btnMode.getGraphic()).setContent(svgData);
        btnMode.setTooltip(new Tooltip(tooltipText));
    }

    private void playNextSong() {
        if (playList.isEmpty()) return;
        int newIndex = currentIndex;
        if (currentMode == PlayMode.LOOP_ONE) {
            playSong(currentIndex);
            return;
        } else if (currentMode == PlayMode.SHUFFLE) {
            if (playList.size() > 1) {
                do { newIndex = new Random().nextInt(playList.size()); } while (newIndex == currentIndex);
            }
        } else {
            newIndex = currentIndex + 1;
            if (newIndex >= playList.size()) newIndex = 0;
        }
        playSong(newIndex);
    }

    private void playPrev() {
        if (playList.isEmpty()) return;
        int newIndex;
        if (currentMode == PlayMode.SHUFFLE) newIndex = new Random().nextInt(playList.size());
        else {
            newIndex = currentIndex - 1;
            if (newIndex < 0) newIndex = playList.size() - 1;
        }
        playSong(newIndex);
    }

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

    // 使用 UTF-8 读取播放列表
    private void loadSavedPlaylist() {
        try {
            File f = new File("playlist.txt");
            if (f.exists()) {
                List<String> lines = Files.readAllLines(Paths.get(f.toURI()), StandardCharsets.UTF_8);
                for (String path : lines) {
                    File file = new File(path);
                    if (file.exists()) addToPlaylistSafe(file);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // 绝对路径去重
    private void addToPlaylistSafe(File file) {
        boolean exists = playList.stream().anyMatch(f -> f.getAbsolutePath().equals(file.getAbsolutePath()));
        if (!exists) {
            playList.add(file);
            listModel.add(file.getName());
        }
    }

    private void hideScrollBars(Scene scene) {
        String css = ".list-view .scroll-bar:horizontal {-fx-pref-height: 0;-fx-opacity: 0;} .list-view .scroll-bar:vertical {-fx-pref-width: 0;-fx-opacity: 0;} .list-view .corner {-fx-background-color: transparent;}";
        try { scene.getStylesheets().add("data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes("UTF-8"))); } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupDragAndDrop(Scene scene) {
        scene.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });
        scene.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                for (File f : files) {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(".mp3") || name.endsWith(".wav")) addToPlaylistSafe(f);
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    // 使用 UTF-8 写入播放列表
    @Override public void stop() throws Exception {
        super.stop();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("playlist.txt"), StandardCharsets.UTF_8))) {
            for (File f : playList) { writer.write(f.getAbsolutePath()); writer.newLine(); }
        }
    }

    private Button createPlayButton() { return new Button("▶"); }

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
