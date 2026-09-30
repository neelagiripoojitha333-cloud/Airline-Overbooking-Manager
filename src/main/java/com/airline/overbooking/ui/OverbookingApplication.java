package com.airline.overbooking.ui;

import com.airline.overbooking.model.BookingStatus;
import com.airline.overbooking.model.FareClass;
import com.airline.overbooking.model.Flight;
import com.airline.overbooking.model.Passenger;
import com.airline.overbooking.repository.SqliteBookingRepository;
import com.airline.overbooking.repository.SqliteDatabase;
import com.airline.overbooking.repository.SqliteFlightRepository;
import com.airline.overbooking.repository.SqliteBookingRepository.BookingRecord;
import com.airline.overbooking.service.BookingManagementService;
import com.airline.overbooking.service.CabinLayoutService;
import com.airline.overbooking.service.CabinLayoutService.Seat;
import com.airline.overbooking.service.FlightManagementService;
import com.airline.overbooking.service.FixedFlightCatalog;
import com.airline.overbooking.service.FixedFlightCatalog.ScheduledFlight;
import com.airline.overbooking.service.NoShowPredictionService;
import com.airline.overbooking.service.OverbookingRecommendationService;
import javafx.application.Application;
import javafx.animation.FadeTransition;
import javafx.animation.PathTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.Group;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

public class OverbookingApplication extends Application {
    private final ListView<ScheduledFlight> flightList = new ListView<>();
    private final TableView<BookingRecord> bookingTable = new TableView<>();
        private final GridPane seatMapGrid = new GridPane();
    private final Label flightTitle = new Label("Select a flight");
    private final Label flightRoute = new Label("");
    private final Label capacityValue = new Label("-");
    private final Label recordsValue = new Label("-");
        private final Label confirmedValue = new Label("-");
        private final Label recommendationValue = new Label("Select a flight");
        private final Label recommendationDetails = new Label("");
        private final Label seatSelectionInfo = new Label("Choose a seat to begin booking");
        private final Button addBookingButton = new Button("Book selected seat");
    private final Label statusMessage = new Label("Ready");
    private final Button manageFlightsButton = new Button("Manage Flights");
    private final Button editBookingButton = new Button("Edit booking");
    private final Button cancelBookingButton = new Button("Cancel booking");
    private final Button predictButton = new Button("Estimate no-show");
    private final Button recommendationButton = new Button("Calculate safe limit");

    private FlightManagementService flightService;
    private BookingManagementService bookingService;
    private NoShowPredictionService predictionService;
    private OverbookingRecommendationService recommendationService;
    private final CabinLayoutService cabinLayoutService = new CabinLayoutService();
    private final FixedFlightCatalog fixedFlightCatalog = new FixedFlightCatalog();
    private Flight selectedFlight;
    private List<BookingRecord> visibleBookings = List.of();
    private String selectedSeatNumber;

    @Override
    public void start(Stage stage) throws Exception {
        Path databasePath = Path.of(System.getProperty(
                "airline.database", "data/airline-overbooking.db"));
        SqliteDatabase database = new SqliteDatabase(databasePath);
        SqliteBookingRepository bookingRepository = new SqliteBookingRepository(database);
        SqliteFlightRepository flightRepository = new SqliteFlightRepository(database, bookingRepository);
        flightService = new FlightManagementService(flightRepository);
        bookingService = new BookingManagementService(bookingRepository);
        predictionService = new NoShowPredictionService(
                System.getProperty("airline.python", "python"),
                Path.of("src/main/python/no_show_predictor.py"));
            recommendationService = new OverbookingRecommendationService(
                (passenger, days) -> predictionService.estimateProbability(passenger, 0, 0, days));

        BorderPane root = buildLayout();
        StackPane applicationRoot = new StackPane(root);
        Scene scene = new Scene(applicationRoot, 1510, 940);
        var stylesheet = getClass().getResource("/css/overbooking.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("Airline Overbooking Manager");
        stage.setMinWidth(1260);
        stage.setMinHeight(800);
        stage.setScene(scene);
        stage.show();
        refreshFlights(null);

        Preferences preferences = Preferences.userNodeForPackage(OverbookingApplication.class);
        if (!preferences.getBoolean("landingIntroShown", false)) {
            StackPane intro = createLandingIntro();
            applicationRoot.getChildren().add(intro);
            playLandingIntro(intro, applicationRoot, preferences);
        }
    }

    private StackPane createLandingIntro() {
        AnchorPane scene = new AnchorPane();
        scene.getStyleClass().add("landing-intro");

        Label title = new Label("AIR INDIA");
        title.getStyleClass().add("intro-brand");
        Label subtitle = new Label("A better journey begins here");
        subtitle.getStyleClass().add("intro-subtitle");
        VBox copy = new VBox(9, title, subtitle);
        copy.setLayoutX(72);
        copy.setLayoutY(58);

        Group cloudOne = createCloud();
        cloudOne.setLayoutX(120);
        cloudOne.setLayoutY(250);
        Group cloudTwo = createCloud();
        cloudTwo.setScaleX(0.72);
        cloudTwo.setScaleY(0.72);
        cloudTwo.setLayoutX(680);
        cloudTwo.setLayoutY(160);

        Rectangle runway = new Rectangle(0, 76);
        runway.getStyleClass().add("intro-runway");
        AnchorPane.setLeftAnchor(runway, 0.0);
        AnchorPane.setRightAnchor(runway, 0.0);
        AnchorPane.setBottomAnchor(runway, 0.0);
        Label runwayMarking = new Label("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        runwayMarking.getStyleClass().add("runway-marking");
        AnchorPane.setLeftAnchor(runwayMarking, 40.0);
        AnchorPane.setBottomAnchor(runwayMarking, 25.0);
        Circle touchdownGlow = new Circle(22);
        touchdownGlow.getStyleClass().add("touchdown-glow");
        touchdownGlow.setOpacity(0);
        AnchorPane.setRightAnchor(touchdownGlow, 135.0);
        AnchorPane.setBottomAnchor(touchdownGlow, 57.0);

        Label airplane = new Label("\u2708");
        airplane.getStyleClass().add("intro-airplane");
        airplane.setLayoutX(-90);
        airplane.setLayoutY(190);

        scene.getChildren().addAll(runway, runwayMarking, touchdownGlow, cloudOne,
                cloudTwo, copy, airplane);
        return new StackPane(scene);
    }

    private Group createCloud() {
        Circle left = new Circle(25);
        left.setCenterX(28);
        left.setCenterY(34);
        Circle center = new Circle(34);
        center.setCenterX(62);
        center.setCenterY(23);
        Circle right = new Circle(25);
        right.setCenterX(96);
        right.setCenterY(35);
        Rectangle base = new Rectangle(24, 30, 78, 29);
        Group cloud = new Group(left, center, right, base);
        cloud.getStyleClass().add("intro-cloud");
        return cloud;
    }

    private void playLandingIntro(StackPane intro, StackPane applicationRoot, Preferences preferences) {
        AnchorPane scene = (AnchorPane) intro.getChildren().get(0);
        Label airplane = (Label) scene.getChildren().get(scene.getChildren().size() - 1);
        Circle touchdownGlow = (Circle) scene.getChildren().get(2);
        javafx.scene.shape.Path flightPath = new javafx.scene.shape.Path(
                new MoveTo(-50, 210),
                new CubicCurveTo(300, 40, 590, 95, 830, 260),
                new CubicCurveTo(1040, 390, 1190, 520, 1380, 790));
        PathTransition flight = new PathTransition(javafx.util.Duration.seconds(4.0), flightPath, airplane);
        flight.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
        flight.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);

        Group firstCloud = (Group) scene.getChildren().get(3);
        Group secondCloud = (Group) scene.getChildren().get(4);
        TranslateTransition firstDrift = new TranslateTransition(javafx.util.Duration.seconds(5.2), firstCloud);
        firstDrift.setByX(190);
        firstDrift.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        TranslateTransition secondDrift = new TranslateTransition(javafx.util.Duration.seconds(5.7), secondCloud);
        secondDrift.setByX(-150);
        secondDrift.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        firstDrift.play();
        secondDrift.play();

        flight.setOnFinished(event -> {
            FadeTransition landing = new FadeTransition(javafx.util.Duration.millis(420), touchdownGlow);
            landing.setFromValue(0.0);
            landing.setToValue(0.8);
            FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(700), intro);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setDelay(javafx.util.Duration.millis(260));
            fade.setOnFinished(done -> {
                applicationRoot.getChildren().remove(intro);
                preferences.putBoolean("landingIntroShown", true);
            });
            landing.setOnFinished(landed -> fade.play());
            landing.play();
        });
        flight.play();
    }

    private BorderPane buildLayout() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(buildHeader());

        flightList.getStyleClass().add("flight-list");
        flightList.setPlaceholder(new Label("No flights yet"));
        flightList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ScheduledFlight schedule, boolean empty) {
                super.updateItem(schedule, empty);
                if (empty || schedule == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                VBox content = createFlightCard(schedule, isSelected());
                setText(null);
                setGraphic(content);
            }
        });
        flightList.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showFlight(selected));

        manageFlightsButton.getStyleClass().add("button-secondary");
        manageFlightsButton.setMaxWidth(Double.MAX_VALUE);

        VBox sidebar = new VBox(12, new Label("AVAILABLE FLIGHTS"), flightList);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(18));
        sidebar.setPrefWidth(420);
        VBox.setVgrow(flightList, Priority.ALWAYS);

        VBox mainContent = buildFlightContent();
        HBox workspace = new HBox(18, sidebar, mainContent);
        workspace.setPadding(new Insets(20));
        HBox.setHgrow(mainContent, Priority.ALWAYS);
        root.setCenter(workspace);

        statusMessage.getStyleClass().add("status-message");
        BorderPane.setMargin(statusMessage, new Insets(0, 24, 14, 24));
        root.setBottom(statusMessage);
        return root;
    }

    private HBox buildHeader() {
        Label brand = new Label("AIRLINE  /  OPERATIONS");
        brand.getStyleClass().add("brand-label");
        Label phase = new Label("BOOKING CONTROL");
        phase.getStyleClass().add("header-tag");
        manageFlightsButton.getStyleClass().add("button-secondary");
        manageFlightsButton.setOnAction(event -> openFlightManager());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(18, brand, spacer, manageFlightsButton, phase);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.getStyleClass().add("app-header");
        return header;
    }

    private VBox buildFlightContent() {
        flightTitle.getStyleClass().add("page-title");
        flightRoute.getStyleClass().add("page-subtitle");

        VBox titleGroup = new VBox(5, flightTitle, flightRoute);
        VBox capacityCard = createStatCard("SEAT CAPACITY", capacityValue);
        VBox bookingsCard = createStatCard("BOOKING RECORDS", recordsValue);
        VBox confirmedCard = createStatCard("CONFIRMED BOOKINGS", confirmedValue);
        HBox stats = new HBox(12, capacityCard, bookingsCard, confirmedCard);

        Label capacityNote = new Label(
                "Modelled risk ceiling: 1%, assuming independent passenger outcomes. History defaults to 0 prior no-shows/flights; new bookings assume FIRST fare class.");
        capacityNote.getStyleClass().add("quiet-note");
        capacityNote.setWrapText(true);

        Label recommendationHeading = new Label("OVERBOOKING RECOMMENDATION");
        recommendationHeading.getStyleClass().add("stat-heading");
        recommendationValue.getStyleClass().add("recommendation-value");
        recommendationValue.setWrapText(true);
        recommendationDetails.getStyleClass().add("recommendation-details");
        recommendationDetails.setWrapText(true);
        recommendationButton.getStyleClass().add("button-accent");
        recommendationButton.setDisable(true);
        recommendationButton.setOnAction(event -> calculateRecommendation());
        HBox recommendationHeader = new HBox(12, recommendationHeading, new Region(), recommendationButton);
        recommendationHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(recommendationHeader.getChildren().get(1), Priority.ALWAYS);
        VBox recommendationPanel = new VBox(9, recommendationHeader, recommendationValue,
            recommendationDetails);
        recommendationPanel.getStyleClass().add("recommendation-panel");

        addBookingButton.getStyleClass().add("button-primary");
        addBookingButton.setDisable(true);
        addBookingButton.setOnAction(event -> addBooking());
        editBookingButton.setDisable(true);
        cancelBookingButton.getStyleClass().add("button-danger");
        cancelBookingButton.setDisable(true);
        predictButton.getStyleClass().add("button-accent");
        predictButton.setDisable(true);
        editBookingButton.setOnAction(event -> editBooking());
        cancelBookingButton.setOnAction(event -> cancelBooking());
        predictButton.setOnAction(event -> estimateNoShow());
        bookingTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> setBookingActionsEnabled(selected != null));
        configureBookingTable();

        HBox workspace = new HBox(16, buildSeatMapPanel(), buildBookingPanel());
        VBox.setVgrow(workspace, Priority.ALWAYS);

        VBox content = new VBox(15, titleGroup, stats, capacityNote, recommendationPanel, workspace);
        content.getStyleClass().add("main-content");
        return content;
    }

    private VBox createFlightCard(ScheduledFlight schedule, boolean selected) {
                VBox card = new VBox(8);
                card.getStyleClass().add("flight-card");
                if (selected) {
                    card.getStyleClass().add("flight-card-selected");
                }
                card.setMaxWidth(Double.MAX_VALUE);

                Label airline = new Label("AIR INDIA  /  " + schedule.flight().getFlightNumber());
                airline.getStyleClass().add("flight-card-number");
                Label route = new Label(schedule.originFlag() + "  " + schedule.originCity()
                        + "  →  " + schedule.destinationFlag() + "  " + schedule.destinationCity());
                route.getStyleClass().add("flight-card-route");
                Label countryCodes = new Label(schedule.flight().getOrigin() + "  ·  " + schedule.originCountry()
                        + "     →     " + schedule.flight().getDestination() + "  ·  "
                        + schedule.destinationCountry());
                countryCodes.getStyleClass().add("flight-card-airports");

                Label departure = new Label("DEPARTURE\n" + schedule.flight().getOrigin() + "  "
                        + formatTime(schedule.departureTime()) + "\n" + formatDate(schedule.departureDate()));
                departure.getStyleClass().add("flight-time-block");
                Label arrival = new Label("ARRIVAL\n" + schedule.flight().getDestination() + "  "
                        + formatTime(schedule.arrivalTime()) + "\n" + formatDate(schedule.arrivalDate()));
                arrival.getStyleClass().add("flight-time-block");
                Label duration = new Label("DURATION\n" + formatDuration(schedule.duration()));
                duration.getStyleClass().add("flight-duration-block");
                HBox scheduleLine = new HBox(7, departure, arrival, duration);
                scheduleLine.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(departure, Priority.ALWAYS);
                HBox.setHgrow(arrival, Priority.ALWAYS);

                GridPane fares = new GridPane();
                fares.setHgap(6);
                fares.setVgap(5);
                fares.add(createFareCell("ECONOMY", FareClass.ECONOMY, schedule), 0, 0);
                fares.add(createFareCell("PREMIUM ECONOMY", FareClass.PREMIUM_ECONOMY, schedule), 1, 0);
                fares.add(createFareCell("BUSINESS", FareClass.BUSINESS, schedule), 0, 1);
                fares.add(createFareCell("FIRST CLASS", FareClass.FIRST, schedule), 1, 1);

                card.getChildren().addAll(airline, route, countryCodes, scheduleLine, fares);
                return card;
            }

            private VBox createFareCell(String heading, FareClass fareClass, ScheduledFlight schedule) {
                Label name = new Label(heading);
                name.getStyleClass().add("fare-name");
                Label price = new Label(schedule.currency() + "  \u20B9"
                    + String.format("%,d", schedule.fares().get(fareClass)));
                price.getStyleClass().add("fare-price");
                VBox fare = new VBox(3, name, price);
                fare.getStyleClass().addAll("fare-cell", fareStyleClass(fareClass));
                return fare;
            }

            private static String fareStyleClass(FareClass fareClass) {
                return switch (fareClass) {
                    case ECONOMY -> "fare-economy";
                    case PREMIUM_ECONOMY -> "fare-premium";
                    case BUSINESS -> "fare-business";
                    case FIRST -> "fare-first";
                };
            }

            private static String formatDate(java.time.LocalDate date) {
                return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            }

            private static String formatTime(java.time.LocalTime time) {
                return time.format(DateTimeFormatter.ofPattern("hh:mm a"));
            }

            private static String formatDuration(java.time.Duration duration) {
                long hours = duration.toHours();
                long minutes = duration.toMinutesPart();
                return hours + "h " + String.format("%02dm", minutes);
            }

            private VBox buildSeatMapPanel() {
        Label title = new Label("Choose your seat");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Six seats per row  /  aisle between C and D");
        subtitle.getStyleClass().add("quiet-note");
        seatSelectionInfo.getStyleClass().add("seat-selection-info");

        seatMapGrid.setAlignment(Pos.CENTER);
        seatMapGrid.setHgap(7);
        seatMapGrid.setVgap(8);
        ScrollPane seatScroll = new ScrollPane(seatMapGrid);
        seatScroll.setFitToWidth(true);
        seatScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        seatScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        seatScroll.setPrefViewportHeight(430);
        seatScroll.getStyleClass().add("seat-map-scroll");
seatScroll.setPrefViewportHeight(300);
VBox.setVgrow(seatScroll, Priority.NEVER);
        HBox legend = new HBox(12,
                createSeatLegend("FIRST", "seat-first"),
                createSeatLegend("BUSINESS", "seat-business"),
                createSeatLegend("PREMIUM", "seat-premium"),
                createSeatLegend("ECONOMY", "seat-economy"),
                createSeatLegend("TAKEN", "seat-occupied"));
        legend.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(
        12,
        title,
        subtitle,
        legend,
        seatSelectionInfo,
        seatScroll,
        addBookingButton
);

panel.getStyleClass().add("seat-map-panel");

addBookingButton.setMaxWidth(Double.MAX_VALUE);
addBookingButton.setPrefHeight(42);
addBookingButton.setVisible(true);
addBookingButton.setManaged(true);

VBox.setVgrow(seatScroll, Priority.NEVER);

panel.setMinWidth(470);
HBox.setHgrow(panel, Priority.ALWAYS);

return panel;
            }

    private HBox createSeatLegend(String label, String seatStyleClass) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll("seat-legend-swatch", seatStyleClass);
        Label text = new Label(label);
        text.getStyleClass().add("seat-legend-label");
        return new HBox(5, swatch, text);
    }

    private VBox buildBookingPanel() {
        Label title = new Label("Bookings");
        title.getStyleClass().add("section-title");
        HBox actions = new HBox(7, editBookingButton, cancelBookingButton, predictButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(12, title, actions, bookingTable);
        panel.getStyleClass().add("booking-panel");
        panel.setPrefWidth(390);
        panel.setMinWidth(330);
        VBox.setVgrow(bookingTable, Priority.ALWAYS);
        HBox.setHgrow(panel, Priority.NEVER);
        return panel;
    }

    private VBox createStatCard(String heading, Label value) {
        Label title = new Label(heading);
        title.getStyleClass().add("stat-heading");
        value.getStyleClass().add("stat-value");
        VBox card = new VBox(8, title, value);
        card.getStyleClass().add("stat-card");
        card.setMinWidth(180);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void configureBookingTable() {
        TableColumn<BookingRecord, String> bookingId = new TableColumn<>("Booking ID");
        bookingId.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().bookingId()));

        TableColumn<BookingRecord, String> seat = new TableColumn<>("Seat");
        seat.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
            cell.getValue().seatNumber() == null ? "-" : cell.getValue().seatNumber()));

        TableColumn<BookingRecord, String> passenger = new TableColumn<>("Passenger");
        passenger.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().passenger().getName()));

        TableColumn<BookingRecord, String> passengerId = new TableColumn<>("Passenger ID");
        passengerId.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().passenger().getPassengerId()));

        TableColumn<BookingRecord, String> fareClass = new TableColumn<>("Fare");
        fareClass.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().passenger().getFareClass().toString().replace('_', ' ')));

        TableColumn<BookingRecord, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().passenger().getBookingStatus().toString()));

        bookingId.setPrefWidth(140);
        seat.setPrefWidth(68);
        passenger.setPrefWidth(180);
        passengerId.setPrefWidth(140);
        fareClass.setPrefWidth(145);
        status.setPrefWidth(125);
        bookingTable.getColumns().setAll(List.of(bookingId, passenger, passengerId, fareClass, status));
        bookingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        bookingTable.setPlaceholder(new Label("Select a flight to view its bookings"));
        bookingTable.getStyleClass().add("booking-table");
    }

    private void refreshFlights(String preferredFlightNumber) {
        try {
            List<ScheduledFlight> flights = new java.util.ArrayList<>();
            for (ScheduledFlight scheduledFlight : fixedFlightCatalog.flights()) {
                Flight storedFlight = flightService.findFlight(scheduledFlight.flight().getFlightNumber())
                        .orElse(null);
                if (storedFlight == null) {
                    flightService.createFlight(scheduledFlight.flight());
                    storedFlight = scheduledFlight.flight();
                }
                flights.add(scheduledFlight.withStoredFlight(storedFlight));
            }
            flightList.setItems(FXCollections.observableArrayList(flights));

            int preferredIndex = -1;
            if (preferredFlightNumber != null) {
                for (int index = 0; index < flights.size(); index++) {
                    if (flights.get(index).flight().getFlightNumber().equals(preferredFlightNumber)) {
                        preferredIndex = index;
                        break;
                    }
                }
            }
            flightList.getSelectionModel().select(preferredIndex >= 0 ? preferredIndex : 0);
        } catch (SQLException exception) {
            showError("Could not load fixed flights", exception);
        }
    }

    private void showFlight(ScheduledFlight scheduledFlight) {
        boolean selected = scheduledFlight != null;
        addBookingButton.setDisable(!selected);
        if (!selected) {
            selectedFlight = null;
            visibleBookings = List.of();
            selectedSeatNumber = null;
            flightTitle.setText("Select a flight");
            flightRoute.setText("");
            capacityValue.setText("-");
            recordsValue.setText("-");
            confirmedValue.setText("-");
            recommendationButton.setDisable(true);
            recommendationValue.setText("Select a flight");
            recommendationDetails.setText("");
            seatSelectionInfo.setText("Choose a flight to view seats");
            addBookingButton.setDisable(true);
            bookingTable.getItems().clear();
            renderSeatMap();
            setBookingActionsEnabled(false);
            return;
        }

        Flight flight = scheduledFlight.flight();
        selectedFlight = flight;
        selectedSeatNumber = null;
        flightTitle.setText(flight.getFlightNumber());
        flightRoute.setText(scheduledFlight.originCity() + " (" + flight.getOrigin() + ")  to  "
            + scheduledFlight.destinationCity() + " (" + flight.getDestination() + ")");
        capacityValue.setText(Integer.toString(flight.getCapacity()));
        recordsValue.setText(Integer.toString(flight.getBookings().size()));
        int confirmedBookings = (int) flight.getBookings().stream()
                .filter(passenger -> passenger.getBookingStatus() == BookingStatus.CONFIRMED)
                .count();
        confirmedValue.setText(Integer.toString(confirmedBookings));
        recommendationButton.setDisable(false);
        recommendationValue.setText("Not calculated for this flight");
        recommendationDetails.setText("");
        seatSelectionInfo.setText("Choose an available seat to start a booking");
        addBookingButton.setDisable(true);
        try {
            visibleBookings = bookingService.listBookings(flight.getFlightNumber());
            bookingTable.setItems(FXCollections.observableArrayList(visibleBookings));
            renderSeatMap();
        } catch (SQLException exception) {
            showError("Could not load bookings", exception);
        }
        setBookingActionsEnabled(bookingTable.getSelectionModel().getSelectedItem() != null);
    }

    private void renderSeatMap() {
        seatMapGrid.getChildren().clear();
        selectedSeatNumber = selectedFlight == null ? null : selectedSeatNumber;
        if (selectedFlight == null) {
            seatSelectionInfo.setText("Choose a flight to view seats");
            addBookingButton.setDisable(true);
            return;
        }

        String[] letters = {"A", "B", "C", "", "D", "E", "F"};
        seatMapGrid.add(new Label("ROW"), 0, 0);
        for (int column = 0; column < letters.length; column++) {
            if (!letters[column].isEmpty()) {
                Label heading = new Label(letters[column]);
                heading.getStyleClass().add("seat-column-header");
                seatMapGrid.add(heading, column + 1, 0);
            }
        }

        Set<String> occupiedSeats = new HashSet<>();
        for (BookingRecord booking : visibleBookings) {
            BookingStatus status = booking.passenger().getBookingStatus();
            if (booking.seatNumber() != null
                    && (status == BookingStatus.CONFIRMED || status == BookingStatus.BOARDED)) {
                occupiedSeats.add(booking.seatNumber());
            }
        }

        List<Seat> seats = cabinLayoutService.createSeats(selectedFlight.getCapacity());
        for (Seat seat : seats) {
            Button seatButton = new Button(seat.seatNumber());
            seatButton.getStyleClass().addAll("seat-button", seatStyleClass(seat.fareClass()));
            seatButton.setAccessibleText(seat.fareClass().toString().replace('_', ' ')
                    + " seat " + seat.seatNumber());
            seatButton.setPrefSize(43, 31);
            seatButton.setMinSize(43, 31);
            int column = seatColumn(seat.letter());
            int row = seat.row();
            if (occupiedSeats.contains(seat.seatNumber())) {
                seatButton.getStyleClass().add("seat-occupied");
                seatButton.setDisable(true);
                visibleBookings.stream()
                        .filter(booking -> seat.seatNumber().equals(booking.seatNumber()))
                        .findFirst()
                        .ifPresent(booking -> seatButton.setTooltip(new Tooltip(
                                booking.passenger().getName() + "  /  " + booking.bookingId())));
            } else {
                if (seat.seatNumber().equals(selectedSeatNumber)) {
                    seatButton.getStyleClass().add("seat-selected");
                }
                seatButton.setOnAction(event -> selectSeat(seat));
            }
            seatMapGrid.add(seatButton, column, row);
        }

        for (int row = 1; row <= (selectedFlight.getCapacity() + 5) / 6; row++) {
            Label rowNumber = new Label(Integer.toString(row));
            rowNumber.getStyleClass().add("seat-row-number");
            seatMapGrid.add(rowNumber, 0, row);
        }
        if (seats.isEmpty()) {
            Label empty = new Label("No seats defined for this flight");
            empty.getStyleClass().add("quiet-note");
            seatMapGrid.add(empty, 1, 1, 7, 1);
        }
    }

    private void selectSeat(Seat seat) {
        selectedSeatNumber = seat.seatNumber();
        seatSelectionInfo.setText("Selected " + seat.seatNumber() + "  /  "
                + seat.fareClass().toString().replace('_', ' '));
        addBookingButton.setDisable(false);
        renderSeatMap();
    }

    private static int seatColumn(char letter) {
        return switch (letter) {
            case 'A' -> 1;
            case 'B' -> 2;
            case 'C' -> 3;
            case 'D' -> 5;
            case 'E' -> 6;
            case 'F' -> 7;
            default -> throw new IllegalArgumentException("Unknown seat letter: " + letter);
        };
    }

    private static String seatStyleClass(FareClass fareClass) {
        return switch (fareClass) {
            case FIRST -> "seat-first";
            case BUSINESS -> "seat-business";
            case PREMIUM_ECONOMY -> "seat-premium";
            case ECONOMY -> "seat-economy";
        };
    }

    private void setBookingActionsEnabled(boolean hasSelection) {
        editBookingButton.setDisable(!hasSelection);
        cancelBookingButton.setDisable(!hasSelection);
        predictButton.setDisable(!hasSelection);
    }

    private void openFlightManager() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Flights");
        dialog.setHeaderText("Manage stored routes separately from the fixed booking menu");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ListView<Flight> managedFlights = new ListView<>();
        managedFlights.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Flight flight, boolean empty) {
                super.updateItem(flight, empty);
                setText(empty || flight == null ? null : flight.getFlightNumber() + "  |  "
                        + flight.getOrigin() + " to " + flight.getDestination()
                        + "  |  " + flight.getCapacity() + " seats");
            }
        });
        managedFlights.setPrefHeight(300);
        Button addRoute = new Button("Add route");
        Button editRoute = new Button("Edit route");
        Button deleteRoute = new Button("Delete route");
        addRoute.getStyleClass().add("button-primary");
        editRoute.setDisable(true);
        deleteRoute.getStyleClass().add("button-danger");
        deleteRoute.setDisable(true);
        managedFlights.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    editRoute.setDisable(selected == null);
                    deleteRoute.setDisable(selected == null);
                });

        Runnable refreshManagedFlights = () -> loadManagedFlights(managedFlights);
        addRoute.setOnAction(event -> showFlightDialog(null).ifPresent(flight -> {
            try {
                flightService.createFlight(flight);
                refreshManagedFlights.run();
                refreshFlights(flight.getFlightNumber());
            } catch (SQLException exception) {
                showError("Could not add flight", exception);
            }
        }));
        editRoute.setOnAction(event -> {
            Flight selected = managedFlights.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showFlightDialog(selected).ifPresent(flight -> {
                    try {
                        if (flightService.updateFlight(flight)) {
                            refreshManagedFlights.run();
                            refreshFlights(flight.getFlightNumber());
                        }
                    } catch (SQLException exception) {
                        showError("Could not update flight", exception);
                    }
                });
            }
        });
        deleteRoute.setOnAction(event -> {
            Flight selected = managedFlights.getSelectionModel().getSelectedItem();
            if (selected == null || !confirm("Delete stored route " + selected.getFlightNumber() + "?")) {
                return;
            }
            try {
                if (flightService.deleteFlight(selected.getFlightNumber())) {
                    refreshManagedFlights.run();
                    refreshFlights(null);
                }
            } catch (SQLException exception) {
                showError("Could not delete flight", exception);
            }
        });

        HBox actions = new HBox(8, addRoute, editRoute, deleteRoute);
        VBox content = new VBox(12, managedFlights, actions);
        dialog.getDialogPane().setContent(content);
        refreshManagedFlights.run();
        dialog.showAndWait();
    }

    private void loadManagedFlights(ListView<Flight> managedFlights) {
        try {
            managedFlights.setItems(FXCollections.observableArrayList(flightService.listFlights()));
        } catch (SQLException exception) {
            showError("Could not load managed flights", exception);
        }
    }

    private Optional<Flight> showFlightDialog(Flight existing) {
        Dialog<Flight> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add flight" : "Edit flight");
        dialog.setHeaderText(existing == null ? "Enter flight details" : "Update route or capacity");
        ButtonType save = new ButtonType(existing == null ? "Add flight" : "Save changes",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField flightNumber = new TextField(existing == null ? "" : existing.getFlightNumber());
        flightNumber.setPromptText("e.g. AB123");
        flightNumber.setDisable(existing != null);
        TextField origin = new TextField(existing == null ? "" : existing.getOrigin());
        origin.setPromptText("Origin airport");
        TextField destination = new TextField(existing == null ? "" : existing.getDestination());
        destination.setPromptText("Destination airport");
        TextField capacity = new TextField(existing == null ? "" : Integer.toString(existing.getCapacity()));
        capacity.setPromptText("Seat capacity");

        GridPane form = dialogForm();
        addFormRow(form, 0, "Flight number", flightNumber);
        addFormRow(form, 1, "Origin", origin);
        addFormRow(form, 2, "Destination", destination);
        addFormRow(form, 3, "Capacity", capacity);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> {
            if (button != save) {
                return null;
            }
            try {
                int seatCapacity = Integer.parseInt(capacity.getText().trim());
                return new Flight(flightNumber.getText(), origin.getText(), destination.getText(),
                        seatCapacity, existing == null ? List.of() : existing.getBookings());
            } catch (NumberFormatException exception) {
                showError("Invalid capacity", new IllegalArgumentException("Capacity must be a whole number"));
                return null;
            } catch (IllegalArgumentException exception) {
                showError("Invalid flight", exception);
                return null;
            }
        });
        return dialog.showAndWait();
    }

    private void addBooking() {
        if (selectedFlight == null || selectedSeatNumber == null) {
            return;
        }
        showBookingDialog(null, selectedFlight, selectedSeatNumber).ifPresent(form -> {
            try {
                bookingService.createBooking(form.bookingId(), form.flightNumber(), form.passenger(),
                        form.seatNumber());
                refreshFlights(form.flightNumber());
                statusMessage.setText("Booking added");
            } catch (SQLException exception) {
                showError("Could not add booking", exception);
            }
        });
    }

    private void editBooking() {
        BookingRecord selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        showBookingDialog(selected, null, selected.seatNumber()).ifPresent(form -> {
            try {
                if (bookingService.updateBooking(form.bookingId(), form.flightNumber(), form.passenger(),
                        form.seatNumber())) {
                    refreshFlights(form.flightNumber());
                    statusMessage.setText("Booking updated");
                }
            } catch (SQLException exception) {
                showError("Could not update booking", exception);
            }
        });
    }

    private Optional<BookingForm> showBookingDialog(BookingRecord existing, Flight initialFlight,
                                                   String initialSeatNumber) {
        Dialog<BookingForm> dialog = new Dialog<>();
        ComboBox<Flight> flight = new ComboBox<>(FXCollections.observableArrayList(
                loadFlightsForDialog(existing == null ? null : existing.flightNumber())));
        dialog.setTitle(existing == null ? "Add booking" : "Edit booking");
        dialog.setHeaderText(existing == null ? "Enter passenger and booking details" : "Update booking details");
        ButtonType save = new ButtonType(existing == null ? "Add booking" : "Save changes",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField bookingId = new TextField(existing == null ? "" : existing.bookingId());
        bookingId.setPromptText("Unique booking reference");
        bookingId.setDisable(existing != null);
        TextField passengerId = new TextField(existing == null ? "" : existing.passenger().getPassengerId());
        TextField name = new TextField(existing == null ? "" : existing.passenger().getName());
        TextField age = new TextField(existing == null ? "" : Integer.toString(existing.passenger().getAge()));
        ComboBox<FareClass> fareClass = new ComboBox<>(FXCollections.observableArrayList(FareClass.values()));
        fareClass.setValue(existing == null ? fareClassForSeat(initialFlight, initialSeatNumber)
                : existing.passenger().getFareClass());
        ComboBox<BookingStatus> status = new ComboBox<>(
                FXCollections.observableArrayList(BookingStatus.values()));
        status.setValue(existing == null ? BookingStatus.CONFIRMED : existing.passenger().getBookingStatus());
        ComboBox<String> seatNumber = new ComboBox<>();
        seatNumber.setPromptText("Select an available seat");
        seatNumber.setMaxWidth(Double.MAX_VALUE);
        flight.setCellFactory(list -> flightChoiceCell());
        flight.setButtonCell(flightChoiceCell());
        flight.setMaxWidth(Double.MAX_VALUE);

        String preferredFlightNumber = existing != null
                ? existing.flightNumber() : initialFlight == null ? null : initialFlight.getFlightNumber();
        if (preferredFlightNumber != null) {
            flight.getItems().stream()
                    .filter(candidate -> candidate.getFlightNumber().equals(preferredFlightNumber))
                    .findFirst().ifPresent(flight::setValue);
        }

        Runnable refreshSeatChoices = () -> refreshSeatChoices(seatNumber, flight.getValue(),
                fareClass.getValue(), status.getValue(), existing, initialSeatNumber);
        flight.valueProperty().addListener((observable, previous, selected) -> refreshSeatChoices.run());
        fareClass.valueProperty().addListener((observable, previous, selected) -> refreshSeatChoices.run());
        status.valueProperty().addListener((observable, previous, selected) -> refreshSeatChoices.run());
        refreshSeatChoices.run();

        GridPane form = dialogForm();
        addFormRow(form, 0, "Booking ID", bookingId);
        addFormRow(form, 1, "Flight", flight);
        addFormRow(form, 2, "Passenger ID", passengerId);
        addFormRow(form, 3, "Name", name);
        addFormRow(form, 4, "Age", age);
        addFormRow(form, 5, "Fare class", fareClass);
        addFormRow(form, 6, "Status", status);
        addFormRow(form, 7, "Seat", seatNumber);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> {
            if (button != save) {
                return null;
            }
            try {
                if (flight.getValue() == null || fareClass.getValue() == null || status.getValue() == null) {
                    throw new IllegalArgumentException("Choose a flight, fare class, and booking status");
                }
                String chosenSeat = seatNumber.getValue();
                if (requiresAssignedSeat(status.getValue()) && chosenSeat == null) {
                    throw new IllegalArgumentException("Choose an available seat for this booking status");
                }
                Passenger passenger = new Passenger(passengerId.getText(), name.getText(),
                        Integer.parseInt(age.getText().trim()), fareClass.getValue(), status.getValue());
                return new BookingForm(bookingId.getText().trim(), flight.getValue().getFlightNumber(),
                        passenger, requiresAssignedSeat(status.getValue()) ? chosenSeat : null);
            } catch (NumberFormatException exception) {
                showError("Invalid age", new IllegalArgumentException("Age must be a whole number"));
                return null;
            } catch (IllegalArgumentException exception) {
                showError("Invalid booking", exception);
                return null;
            }
        });
        return dialog.showAndWait();
    }

    private void refreshSeatChoices(ComboBox<String> seatChoices, Flight flight, FareClass fareClass,
                                    BookingStatus status, BookingRecord existing, String preferredSeat) {
        boolean seatRequired = status != null && requiresAssignedSeat(status);
        seatChoices.setDisable(!seatRequired);
        if (!seatRequired || flight == null || fareClass == null) {
            seatChoices.setItems(FXCollections.observableArrayList());
            seatChoices.setValue(null);
            return;
        }

        try {
            Set<String> occupiedSeats = new HashSet<>();
            for (BookingRecord booking : bookingService.listBookings(flight.getFlightNumber())) {
                if (booking.seatNumber() == null
                        || (existing != null && existing.bookingId().equals(booking.bookingId()))) {
                    continue;
                }
                if (requiresAssignedSeat(booking.passenger().getBookingStatus())) {
                    occupiedSeats.add(booking.seatNumber());
                }
            }

            List<String> availableSeats = cabinLayoutService.createSeats(flight.getCapacity()).stream()
                    .filter(seat -> seat.fareClass() == fareClass)
                    .map(Seat::seatNumber)
                    .filter(seat -> !occupiedSeats.contains(seat))
                    .toList();
            String currentBookingSeat = existing != null
                    && existing.flightNumber().equals(flight.getFlightNumber())
                    ? existing.seatNumber() : null;
            if (currentBookingSeat != null && cabinLayoutService.createSeats(flight.getCapacity()).stream()
                    .anyMatch(seat -> seat.seatNumber().equals(currentBookingSeat)
                            && seat.fareClass() == fareClass)
                    && !availableSeats.contains(currentBookingSeat)) {
                java.util.ArrayList<String> withCurrentSeat = new java.util.ArrayList<>(availableSeats);
                withCurrentSeat.add(currentBookingSeat);
                availableSeats = List.copyOf(withCurrentSeat);
            }
            seatChoices.setItems(FXCollections.observableArrayList(availableSeats));
            String preferred = currentBookingSeat != null ? currentBookingSeat : preferredSeat;
            if (preferred != null && availableSeats.contains(preferred)) {
                seatChoices.setValue(preferred);
            } else if (seatChoices.getValue() != null && !availableSeats.contains(seatChoices.getValue())) {
                seatChoices.setValue(null);
            }
        } catch (SQLException exception) {
            showError("Could not load available seats", exception);
            seatChoices.setItems(FXCollections.observableArrayList());
            seatChoices.setValue(null);
        }
    }

    private static boolean requiresAssignedSeat(BookingStatus status) {
        return status == BookingStatus.CONFIRMED || status == BookingStatus.BOARDED;
    }

    private FareClass fareClassForSeat(Flight flight, String seatNumber) {
        if (flight == null || seatNumber == null) {
            return FareClass.ECONOMY;
        }
        return cabinLayoutService.createSeats(flight.getCapacity()).stream()
                .filter(seat -> seat.seatNumber().equals(seatNumber))
                .map(Seat::fareClass)
                .findFirst()
                .orElse(FareClass.ECONOMY);
    }

    private List<Flight> loadFlightsForDialog(String existingFlightNumber) {
        try {
            List<Flight> availableFlights = new java.util.ArrayList<>();
            for (ScheduledFlight scheduledFlight : fixedFlightCatalog.flights()) {
                flightService.findFlight(scheduledFlight.flight().getFlightNumber())
                        .ifPresent(availableFlights::add);
            }
            if (existingFlightNumber != null && availableFlights.stream().noneMatch(
                    flight -> flight.getFlightNumber().equals(existingFlightNumber))) {
                flightService.findFlight(existingFlightNumber).ifPresent(availableFlights::add);
            }
            return List.copyOf(availableFlights);
        } catch (SQLException exception) {
            showError("Could not load flights", exception);
            return List.of();
        }
    }

    private ListCell<Flight> flightChoiceCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Flight flight, boolean empty) {
                super.updateItem(flight, empty);
                setText(empty || flight == null ? null : flight.getFlightNumber() + "  "
                        + flight.getOrigin() + " to " + flight.getDestination());
            }
        };
    }

    private void cancelBooking() {
        BookingRecord selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null || !confirm("Cancel booking " + selected.bookingId() + "?")) {
            return;
        }
        try {
            if (bookingService.cancelBooking(selected.bookingId())) {
                refreshFlights(selected.flightNumber());
                statusMessage.setText("Booking cancelled; record retained");
            }
        } catch (SQLException exception) {
            showError("Could not cancel booking", exception);
        }
    }

    private void estimateNoShow() {
        BookingRecord selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Dialog<PredictionInputs> dialog = new Dialog<>();
        dialog.setTitle("No-show estimate");
        dialog.setHeaderText("Supply the passenger's trip history");
        ButtonType estimate = new ButtonType("Estimate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(estimate, ButtonType.CANCEL);
        TextField previousNoShows = new TextField("0");
        TextField previousFlights = new TextField("0");
        TextField daysBeforeDeparture = new TextField("7");
        GridPane form = dialogForm();
        addFormRow(form, 0, "Previous no-shows", previousNoShows);
        addFormRow(form, 1, "Previous flights", previousFlights);
        addFormRow(form, 2, "Days before departure", daysBeforeDeparture);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> {
            if (button != estimate) {
                return null;
            }
            try {
                return new PredictionInputs(Integer.parseInt(previousNoShows.getText().trim()),
                        Integer.parseInt(previousFlights.getText().trim()),
                        Integer.parseInt(daysBeforeDeparture.getText().trim()));
            } catch (NumberFormatException exception) {
                showError("Invalid prediction inputs", new IllegalArgumentException(
                        "History counts and days must be whole numbers"));
                return null;
            }
        });
        dialog.showAndWait().ifPresent(inputs -> runPrediction(selected, inputs));
    }

    private void runPrediction(BookingRecord booking, PredictionInputs inputs) {
        predictButton.setDisable(true);
        statusMessage.setText("Estimating no-show probability...");
        Task<Double> task = new Task<>() {
            @Override
            protected Double call() throws Exception {
                return predictionService.estimateProbability(booking.passenger(), inputs.previousNoShows(),
                        inputs.previousFlights(), inputs.daysBeforeDeparture());
            }
        };
        task.setOnSucceeded(event -> {
            predictButton.setDisable(bookingTable.getSelectionModel().getSelectedItem() == null);
            double probability = task.getValue();
            statusMessage.setText(String.format("No-show estimate for %s: %.1f%%",
                    booking.bookingId(), probability * 100));
            Alert result = new Alert(Alert.AlertType.INFORMATION);
            result.setTitle("No-show estimate");
            result.setHeaderText(booking.passenger().getName());
            result.setContentText(String.format("Estimated probability: %.1f%%", probability * 100));
            result.showAndWait();
        });
        task.setOnFailed(event -> {
            predictButton.setDisable(bookingTable.getSelectionModel().getSelectedItem() == null);
            showError("Could not estimate no-show probability", task.getException());
        });
        Thread worker = new Thread(task, "no-show-prediction");
        worker.setDaemon(true);
        worker.start();
    }

    private void calculateRecommendation() {
        Flight selected = selectedFlight;
        if (selected == null) {
            return;
        }

        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Recommendation inputs");
        dialog.setHeaderText("Set the number of days before departure");
        ButtonType calculate = new ButtonType("Calculate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(calculate, ButtonType.CANCEL);
        TextField daysBeforeDeparture = new TextField("7");
        GridPane form = dialogForm();
        addFormRow(form, 0, "Days before departure", daysBeforeDeparture);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> {
            if (button != calculate) {
                return null;
            }
            try {
                int days = Integer.parseInt(daysBeforeDeparture.getText().trim());
                if (days < 0) {
                    throw new IllegalArgumentException("Days before departure must not be negative");
                }
                return days;
            } catch (NumberFormatException exception) {
                showError("Invalid departure days", new IllegalArgumentException(
                        "Days before departure must be a whole number"));
                return null;
            } catch (IllegalArgumentException exception) {
                showError("Invalid departure days", exception);
                return null;
            }
        });
        dialog.showAndWait().ifPresent(days -> runRecommendation(selected, days));
    }

    private void runRecommendation(Flight flight, int daysBeforeDeparture) {
        recommendationButton.setDisable(true);
        recommendationValue.setText("Estimating confirmed passenger attendance...");
        recommendationDetails.setText("");
        Task<OverbookingRecommendationService.Recommendation> task = new Task<>() {
            @Override
            protected OverbookingRecommendationService.Recommendation call() throws Exception {
                return recommendationService.recommend(flight, daysBeforeDeparture);
            }
        };
        task.setOnSucceeded(event -> {
            Flight currentSelection = selectedFlight;
            recommendationButton.setDisable(currentSelection == null);
            if (currentSelection == null
                    || !currentSelection.getFlightNumber().equals(flight.getFlightNumber())) {
                return;
            }
            OverbookingRecommendationService.Recommendation result = task.getValue();
            String recommendation = result.recommendedAdditionalBookings() == 0
                    ? "No additional bookings recommended."
                    : "Accept up to " + result.recommendedAdditionalBookings() + " additional bookings.";
            if (result.overCapacityProbability() > result.probabilityLimit()) {
                recommendation += " Current risk is already above the limit.";
            }
            recommendationValue.setText(recommendation);
            recommendationDetails.setText(String.format(
                    "Capacity: %d | Confirmed: %d | Expected no-shows: %.2f | Over-capacity risk: %.2f%% (limit %.2f%%)",
                    result.capacity(), result.confirmedBookings(), result.predictedNoShows(),
                    result.overCapacityProbability() * 100, result.probabilityLimit() * 100));
            statusMessage.setText("Recommendation calculated for " + flight.getFlightNumber());
        });
        task.setOnFailed(event -> {
            Flight currentSelection = selectedFlight;
            recommendationButton.setDisable(currentSelection == null);
            if (currentSelection == null
                    || !currentSelection.getFlightNumber().equals(flight.getFlightNumber())) {
                return;
            }
            showError("Could not calculate recommendation", task.getException());
        });
        Thread worker = new Thread(task, "overbooking-recommendation");
        worker.setDaemon(true);
        worker.start();
    }

    private GridPane dialogForm() {
        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        form.setPadding(new Insets(12, 4, 4, 4));
        return form;
    }

    private void addFormRow(GridPane form, int row, String label, javafx.scene.Node control) {
        Label fieldLabel = new Label(label);
        fieldLabel.getStyleClass().add("form-label");
        form.add(fieldLabel, 0, row);
        form.add(control, 1, row);
        if (control instanceof Region region) {
            region.setPrefWidth(270);
        }
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message,
                ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    private void showError(String title, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(error == null || error.getMessage() == null
                ? "An unexpected error occurred" : error.getMessage());
        alert.showAndWait();
        statusMessage.setText(title);
    }

    private record BookingForm(String bookingId, String flightNumber, Passenger passenger, String seatNumber) {
    }

    private record PredictionInputs(int previousNoShows, int previousFlights, int daysBeforeDeparture) {
    }

    public static void main(String[] args) {
        launch(args);
    }
}