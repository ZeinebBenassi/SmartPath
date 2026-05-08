package tn.esprit.interfaces;

import javafx.geometry.Pos;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;

/**
 * Composant de saisie numérique avec Spinner standard
 */
public class NumericSpinner extends HBox {

    private Spinner<Integer> spinner;
    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 5;
    private static final int INITIAL_VALUE = 0;

    public NumericSpinner() {
        initializeUI();
    }

    /**
     * Initialise l'interface utilisateur
     */
    private void initializeUI() {
        this.setAlignment(Pos.CENTER);
        this.setStyle("-fx-background-color: transparent;");

        // Créer le Spinner avec la plage 0-5
        spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                MIN_VALUE, MAX_VALUE, INITIAL_VALUE
        ));
        
        // Style du spinner
        spinner.setPrefWidth(120);
        spinner.setMinWidth(120);
        spinner.setStyle(
            "-fx-padding: 6 8; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 8; " +
            "-fx-background-color: white; "
        );
        
        // Style du champ éditeur du spinner
        spinner.getEditor().setStyle(
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #1D4ED8; " +
            "-fx-alignment: center; " +
            "-fx-control-inner-background: white;"
        );

        this.getChildren().add(spinner);
    }

    /**
     * Récupère la valeur actuellement entrée
     */
    public int getValue() {
        try {
            Integer value = spinner.getValue();
            return value != null ? value : MIN_VALUE;
        } catch (Exception e) {
            return MIN_VALUE;
        }
    }

    /**
     * Définit la valeur du spinner
     */
    public void setValue(int value) {
        if (value < MIN_VALUE) {
            spinner.getValueFactory().setValue(MIN_VALUE);
        } else if (value > MAX_VALUE) {
            spinner.getValueFactory().setValue(MAX_VALUE);
        } else {
            spinner.getValueFactory().setValue(value);
        }
    }

    /**
     * Récupère le Spinner pour accès avancé si nécessaire
     */
    public Spinner<Integer> getSpinner() {
        return spinner;
    }
}
