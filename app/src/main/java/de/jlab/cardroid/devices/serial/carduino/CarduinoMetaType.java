package de.jlab.cardroid.devices.serial.carduino;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum CarduinoMetaType {
    CONNECTION_REQUEST(0x00),
    START_SNIFFING(0x0a),
    STOP_SNIFFING(0x0b),
    SET_UID(0x49),
    CAR_DATA_DEFINITION(0x63),
    BAUD_RATE_REQUEST(0x72);

    private byte command;

    CarduinoMetaType(int command) {
        this.command = (byte)command;
    }

    public boolean equals(byte type) {
        return this.command == type;
    }

    public byte getType() {
        return this.command;
    }

    public static CarduinoSerialPacket createPacket(@NonNull CarduinoMetaType event, @Nullable byte[] payload) {
        return CarduinoPacketType.createPacket(CarduinoPacketType.META, event.command, payload);
    }
}
