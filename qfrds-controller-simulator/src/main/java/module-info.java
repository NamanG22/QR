module com.railway.qfrds {
    requires com.fazecast.jSerialComm;
    requires com.google.zxing;
    requires java.logging;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.railway.qfrds to javafx.fxml;

    exports com.railway.qfrds;
}
