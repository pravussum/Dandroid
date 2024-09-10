package net.mortalsilence.droidfoss.comm;

import static net.mortalsilence.droidfoss.comm.Commands.BATTERY_LIFE;
import static net.mortalsilence.droidfoss.comm.Commands.BOOST;
import static net.mortalsilence.droidfoss.comm.Commands.BYPASS;
import static net.mortalsilence.droidfoss.comm.Commands.CURRENT_TIME;
import static net.mortalsilence.droidfoss.comm.Commands.EXHAUST_TEMPERATURE;
import static net.mortalsilence.droidfoss.comm.Commands.EXTRACT_FAN_SPEED;
import static net.mortalsilence.droidfoss.comm.Commands.EXTRACT_FAN_STEP;
import static net.mortalsilence.droidfoss.comm.Commands.EXTRACT_TEMPERATURE;
import static net.mortalsilence.droidfoss.comm.Commands.FILTER_LIFE;
import static net.mortalsilence.droidfoss.comm.Commands.FILTER_PERIOD;
import static net.mortalsilence.droidfoss.comm.Commands.HUMIDITY;
import static net.mortalsilence.droidfoss.comm.Commands.MANUAL_FAN_SPEED_STEP;
import static net.mortalsilence.droidfoss.comm.Commands.MODE;
import static net.mortalsilence.droidfoss.comm.Commands.NIGHT_COOLING;
import static net.mortalsilence.droidfoss.comm.Commands.OUTDOOR_TEMPERATURE;
import static net.mortalsilence.droidfoss.comm.Commands.REGISTER_0_READ;
import static net.mortalsilence.droidfoss.comm.Commands.REGISTER_1_READ;
import static net.mortalsilence.droidfoss.comm.Commands.REGISTER_1_WRITE;
import static net.mortalsilence.droidfoss.comm.Commands.REGISTER_4_READ;
import static net.mortalsilence.droidfoss.comm.Commands.ROOM_TEMPERATURE;
import static net.mortalsilence.droidfoss.comm.Commands.ROOM_TEMPERATURE_CALCULATED;
import static net.mortalsilence.droidfoss.comm.Commands.SUPPLY_FAN_SPEED;
import static net.mortalsilence.droidfoss.comm.Commands.SUPPLY_FAN_STEP;
import static net.mortalsilence.droidfoss.comm.Commands.SUPPLY_TEMPERATURE;
import static net.mortalsilence.droidfoss.comm.Commands.UNIT_NAME;
import static net.mortalsilence.droidfoss.comm.Commands.UNIT_SERIAL;

import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DanfossAirUnit {

    public static final String TAG = "DanfossAirUnit";

    private final CommunicationController communicationController;

    public DanfossAirUnit(CommunicationController communicationController) {
        this.communicationController = communicationController;
    }

    private boolean getBoolean(byte[] operation, byte[] register) throws IOException {
        return communicationController.sendRobustRequest(operation, register)[0] != 0;
    }

    private short getWord(byte[] operation, byte[] register) throws IOException {
        byte[] resultBytes = communicationController.sendRobustRequest(operation, register);
        return (short) ((resultBytes[0] << 8) | (resultBytes[1] & 0xFF));
    }

    private byte getByte(byte[] operation, byte[] register) throws IOException {
        return communicationController.sendRobustRequest(operation, register)[0];
    }

    private String getString(byte[] operation, byte[] register) throws IOException {
        // length of the string is stored in the first byte
        byte[] result = communicationController.sendRobustRequest(operation, register);
        return new String(result, 1, result[0], StandardCharsets.US_ASCII);
    }

    private void set(byte[] operation, byte[] register, byte value) throws IOException {
        byte[] valueArray = { value };
        communicationController.sendRobustRequest(operation, register, valueArray);
    }

    private short getShort(byte[] operation, byte[] register) throws IOException {
        byte[] result = communicationController.sendRobustRequest(operation, register);
        return (short) ((result[0] << 8) + (result[1] & 0xff));
    }

    private Float getTemperature(byte[] operation, byte[] register)
            throws IOException {
        short shortTemp = getShort(operation, register);
        float temp = ((float) shortTemp) / 100;
        if (temp <= -274 || temp > 100) {
            Log.i(TAG, "Retrieved invalid temperature value from AirUnit: " + temp);
            return null;
        }
        return temp;
    }

    private ZonedDateTime getTimestamp(byte[] operation, byte[] register)
            throws IOException, UnexpectedResponseValueException {
        byte[] result = communicationController.sendRobustRequest(operation, register);
        return asZonedDateTime(result);
    }

    private ZonedDateTime asZonedDateTime(byte[] data) throws UnexpectedResponseValueException {
        int second = data[0];
        int minute = data[1];
        int hour = data[2] & 0x1f;
        int day = data[3] & 0x1f;
        int month = data[4];
        int year = data[5] + 2000;
        try {
            return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.systemDefault());
        } catch (DateTimeException e) {
            String msg = String.format("Ignoring invalid timestamp %s.%s.%s %s:%s:%s", day, month, year, hour, minute,
                    second);
            throw new UnexpectedResponseValueException(msg, e);
        }
    }

    private int asUnsignedByte(byte b) {
        return b & 0xFF;
    }

    private float asPercentByte(byte b) {
        float f = asUnsignedByte(b);
        return f * 100 / 255;
    }

    public String getUnitName() throws IOException {
        return getString(REGISTER_1_READ, UNIT_NAME);
    }

    public String getUnitSerialNumber() throws IOException {
        return String.valueOf(getShort(REGISTER_4_READ, UNIT_SERIAL));
    }
    public Mode getMode() throws IOException {

        return Mode.values()[getByte(REGISTER_1_READ, MODE)];
    }

    public void setMode(Mode mode) throws IOException {
        if(mode == Mode.NA){
            throw new IllegalArgumentException("Setting to mode n/a is not supported");
        }
        setStringTypeRegister(mode, MODE);
    }

    public boolean getBoost() throws IOException {
        return getBoolean(REGISTER_1_READ, BOOST);
    }

    public void setBoost(Boolean boost) throws IOException {
        setOnOffTypeRegister(boost, BOOST);
    }

    private void setStringTypeRegister(Mode mode, byte[] register) throws IOException {
        byte value = (byte) (Mode.valueOf(mode.name()).ordinal());
        set(REGISTER_1_WRITE, register, value);
    }

    private void setOnOffTypeRegister(boolean cmd, byte[] register) throws IOException {
        set(REGISTER_1_WRITE, register, cmd ? (byte) 1 : (byte) 0);
    }

    public short getSupplyFanSpeed() throws IOException {
        return getWord(REGISTER_4_READ, SUPPLY_FAN_SPEED);
    }

    public short getExtractFanSpeed() throws IOException {
        return getWord(REGISTER_4_READ, EXTRACT_FAN_SPEED);
    }

    public int getManualFanStep() throws IOException, UnexpectedResponseValueException {
        byte value = getByte(REGISTER_1_READ, MANUAL_FAN_SPEED_STEP);
        return percentFromTenStepValue(value);
    }

    private int percentFromTenStepValue(byte value) throws UnexpectedResponseValueException {
        if (value < 0 || value > 10) {
            throw new UnexpectedResponseValueException(String.format("Invalid fan step: %d", value));
        }
        return value * 10;
    }

    public void setManualFanStep(int step) throws IOException {
        setPercentTypeRegister(step, MANUAL_FAN_SPEED_STEP);
    }

    private void setPercentTypeRegister(int step, byte[] register) throws IOException {
        byte value = (byte) ((step + 5) / 10);
        set(REGISTER_1_WRITE, register, value);
    }

    public Float getFilterLife() throws IOException {
        return asPercentByte(getByte(REGISTER_1_READ, FILTER_LIFE));
    }

    public Byte getFilterPeriod() throws IOException {
        return getByte(REGISTER_0_READ, FILTER_PERIOD);
    }

    private void setNumberTypeRegister(byte value, byte[] register) throws IOException {
        set(REGISTER_1_WRITE, register, value);
    }

    public byte getSupplyFanStep() throws IOException, UnexpectedResponseValueException {
        return getByte(REGISTER_4_READ, SUPPLY_FAN_STEP);
    }

    public byte getExtractFanStep() throws IOException, UnexpectedResponseValueException {
        return getByte(REGISTER_4_READ, EXTRACT_FAN_STEP);
    }

    public boolean getNightCooling() throws IOException {
        return getBoolean(REGISTER_1_READ, NIGHT_COOLING);
    }

    public boolean getBypass() throws IOException {
        return getBoolean(REGISTER_1_READ, BYPASS);
    }

    public float getHumidity() throws IOException {
        return asPercentByte(getByte(REGISTER_1_READ, HUMIDITY));
    }

    public Float getRoomTemperature() throws IOException {
        return getTemperature(REGISTER_1_READ, ROOM_TEMPERATURE);
    }

    public Float getRoomTemperatureCalculated() throws IOException {
        return getTemperature(REGISTER_0_READ, ROOM_TEMPERATURE_CALCULATED);
    }

    public Float getOutdoorTemperature() throws IOException {
        return getTemperature(REGISTER_1_READ, OUTDOOR_TEMPERATURE);
    }

    public Float getSupplyTemperature() throws IOException {
        return getTemperature(REGISTER_4_READ, SUPPLY_TEMPERATURE);
    }

    public Float getExtractTemperature() throws IOException {
        return getTemperature(REGISTER_4_READ, EXTRACT_TEMPERATURE);
    }

    public Float getExhaustTemperature() throws IOException {
        return getTemperature(REGISTER_4_READ, EXHAUST_TEMPERATURE);
    }

    public int getBatteryLife() throws IOException {
        return asUnsignedByte(getByte(REGISTER_1_READ, BATTERY_LIFE));
    }

    public ZonedDateTime getCurrentTime() throws IOException, UnexpectedResponseValueException {
        return getTimestamp(REGISTER_1_READ, CURRENT_TIME);
    }

    public void setNightCooling(boolean nightCooling) throws IOException {
        setOnOffTypeRegister(nightCooling, NIGHT_COOLING);
    }

    public void setBypass(boolean bypass) throws IOException {
         setOnOffTypeRegister(bypass, BYPASS);
    }
}
