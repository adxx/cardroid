package de.jlab.cardroid.devices.serial.carduino;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.errors.ErrorObservable;

public final class CarduinoErrorParser extends CarduinoPacketParser implements ErrorObservable {

    private Device device;
    private ArrayList<ErrorObservable.ErrorListener> errorHandlers = new ArrayList<>();

    @Override
    protected boolean shouldHandlePacket(CarduinoSerialPacket packet) {
        return CarduinoPacketType.ERROR.equals(packet.getPacketType());
    }

    @Override
    protected void handlePacket(CarduinoSerialPacket packet) {
        int errorNumber = packet.getPacketId();

        for (int i = 0; i < this.errorHandlers.size(); i++) {
            this.errorHandlers.get(i).onError(errorNumber);
        }
    }

    @Override
    public void addListener(@NonNull ErrorListener listener) {
        this.errorHandlers.add(listener);
    }

    @Override
    public void removeListener(@NonNull ErrorListener listener) {
        this.errorHandlers.remove(listener);
    }

    @Override
    public void setDevice(@NonNull Device device) {
        this.device = device;
    }

    @Nullable
    @Override
    public Device getDevice() {
        return this.device;
    }
}
