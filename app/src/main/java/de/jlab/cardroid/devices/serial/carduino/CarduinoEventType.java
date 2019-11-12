package de.jlab.cardroid.devices.serial.carduino;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @Deprecated Hardcoded event types are part of the LegacyDevice. The possible events should
 * be broadcasted by the device on handshake and then made available as an option in the user
 * interface when designing custom screens.
 */
@Deprecated
public enum CarduinoEventType {
    CC_OFF_BUTTON(0x01),
    CC_AC_BUTTON(0x02),
    CC_AUTO_BUTTON(0x03),
    CC_RECIRCULATION_BUTTON(0x04),
    CC_WSH_BUTTON(0x05),
    CC_RWH_BUTTON(0x06),
    CC_MODE_BUTTON(0x07),
    CC_TEMPERATURE(0x08),
    CC_FAN_LEVEL(0x09);

    private byte command;

    CarduinoEventType(int command) {
        this.command = (byte)command;
    }

    public byte getCommand() {
        return this.command;
    }

    public static CarduinoSerialPacket createPacket(@NonNull CarduinoEventType event, @Nullable byte[] payload) {
        return CarduinoPacketType.createPacket(CarduinoPacketType.EVENT, event.command, payload);
    }
}
