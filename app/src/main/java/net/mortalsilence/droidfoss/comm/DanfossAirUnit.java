package net.mortalsilence.droidfoss.comm;

import static net.mortalsilence.droidfoss.comm.Commands.MODE;
import static net.mortalsilence.droidfoss.comm.Commands.REGISTER_1_READ;
import static net.mortalsilence.droidfoss.comm.Commands.REGISTER_1_WRITE;
import static net.mortalsilence.droidfoss.comm.Commands.REGISTER_4_READ;
import static net.mortalsilence.droidfoss.comm.Commands.UNIT_NAME;
import static net.mortalsilence.droidfoss.comm.Commands.UNIT_SERIAL;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DanfossAirUnit {

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

    private float getTemperature(byte[] operation, byte[] register)
            throws IOException, UnexpectedResponseValueException {
        short shortTemp = getShort(operation, register);
        float temp = ((float) shortTemp) / 100;
        if (temp <= -274 || temp > 100) {
            throw new UnexpectedResponseValueException(String.format("Invalid temperature: %s", temp));
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

    private static int asUnsignedByte(byte b) {
        return b & 0xFF;
    }

    private static float asPercentByte(byte b) {
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
        if(mode == Mode.UNKNOWN){
            throw new IllegalArgumentException("Setting to mode unknown is not supported");
        }
        setStringTypeRegister(mode, MODE);
    }

    private void setStringTypeRegister(Mode mode, byte[] register) throws IOException {
        byte value = (byte) (Mode.valueOf(mode.name()).ordinal());
        set(REGISTER_1_WRITE, register, value);
    }

/*
    public PercentType getManualFanStep() throws IOException, UnexpectedResponseValueException {
        byte value = getByte(REGISTER_1_READ, MANUAL_FAN_SPEED_STEP);
        if (value < 0 || value > 10) {
            throw new UnexpectedResponseValueException(String.format("Invalid fan step: %d", value));
        }
        return new PercentType(BigDecimal.valueOf(value * 10));
    }

    public QuantityType<Frequency> getSupplyFanSpeed() throws IOException {
        return new QuantityType<>(BigDecimal.valueOf(getWord(REGISTER_4_READ, SUPPLY_FAN_SPEED)), Units.RPM);
    }

    public QuantityType<Frequency> getExtractFanSpeed() throws IOException {
        return new QuantityType<>(BigDecimal.valueOf(getWord(REGISTER_4_READ, EXTRACT_FAN_SPEED)), Units.RPM);
    }

    public PercentType getSupplyFanStep() throws IOException {
        return new PercentType(BigDecimal.valueOf(getByte(REGISTER_4_READ, SUPPLY_FAN_STEP)));
    }

    public PercentType getExtractFanStep() throws IOException {
        return new PercentType(BigDecimal.valueOf(getByte(REGISTER_4_READ, EXTRACT_FAN_STEP)));
    }

    public OnOffType getBoost() throws IOException {
        return OnOffType.from(getBoolean(REGISTER_1_READ, BOOST));
    }

    public OnOffType getNightCooling() throws IOException {
        return OnOffType.from(getBoolean(REGISTER_1_READ, NIGHT_COOLING));
    }

    public OnOffType getBypass() throws IOException {
        return OnOffType.from(getBoolean(REGISTER_1_READ, BYPASS));
    }

    public QuantityType<Dimensionless> getHumidity() throws IOException {
        BigDecimal value = BigDecimal.valueOf(asPercentByte(getByte(REGISTER_1_READ, HUMIDITY)));
        return new QuantityType<>(value.setScale(1, RoundingMode.HALF_UP), Units.PERCENT);
    }

    public QuantityType<Temperature> getRoomTemperature() throws IOException, UnexpectedResponseValueException {
        return getTemperatureAsDecimalType(REGISTER_1_READ, ROOM_TEMPERATURE);
    }

    public QuantityType<Temperature> getRoomTemperatureCalculated()
            throws IOException, UnexpectedResponseValueException {
        return getTemperatureAsDecimalType(REGISTER_0_READ, ROOM_TEMPERATURE_CALCULATED);
    }

    public QuantityType<Temperature> getOutdoorTemperature() throws IOException, UnexpectedResponseValueException {
        return getTemperatureAsDecimalType(REGISTER_1_READ, OUTDOOR_TEMPERATURE);
    }

    public QuantityType<Temperature> getSupplyTemperature() throws IOException, UnexpectedResponseValueException {
        return getTemperatureAsDecimalType(REGISTER_4_READ, SUPPLY_TEMPERATURE);
    }

    public QuantityType<Temperature> getExtractTemperature() throws IOException, UnexpectedResponseValueException {
        return getTemperatureAsDecimalType(REGISTER_4_READ, EXTRACT_TEMPERATURE);
    }

    public QuantityType<Temperature> getExhaustTemperature() throws IOException, UnexpectedResponseValueException {
        return getTemperatureAsDecimalType(REGISTER_4_READ, EXHAUST_TEMPERATURE);
    }

    private QuantityType<Temperature> getTemperatureAsDecimalType(byte[] operation, byte[] register)
            throws IOException, UnexpectedResponseValueException {
        BigDecimal value = BigDecimal.valueOf(getTemperature(operation, register));
        return new QuantityType<>(value.setScale(1, RoundingMode.HALF_UP), SIUnits.CELSIUS);
    }

    public DecimalType getBatteryLife() throws IOException {
        return new DecimalType(BigDecimal.valueOf(asUnsignedByte(getByte(REGISTER_1_READ, BATTERY_LIFE))));
    }

    public DecimalType getFilterLife() throws IOException {
        BigDecimal value = BigDecimal.valueOf(asPercentByte(getByte(REGISTER_1_READ, FILTER_LIFE)));
        return new DecimalType(value.setScale(1, RoundingMode.HALF_UP));
    }

    public DecimalType getFilterPeriod() throws IOException {
        return new DecimalType(BigDecimal.valueOf(getByte(REGISTER_1_READ, FILTER_PERIOD)));
    }

    public DecimalType setFilterPeriod(Command cmd) throws IOException {
        return setNumberTypeRegister(cmd, FILTER_PERIOD);
    }

    public DateTimeType getCurrentTime() throws IOException, UnexpectedResponseValueException {
        ZonedDateTime timestamp = getTimestamp(REGISTER_1_READ, CURRENT_TIME);
        return new DateTimeType(timestamp);
    }

    public PercentType setManualFanStep(Command cmd) throws IOException {
        return setPercentTypeRegister(cmd, MANUAL_FAN_SPEED_STEP);
    }

    private DecimalType setNumberTypeRegister(Command cmd, byte[] register) throws IOException {
        if (cmd instanceof DecimalType decimalCommand) {
            byte value = (byte) decimalCommand.intValue();
            set(REGISTER_1_WRITE, register, value);
        }
        return new DecimalType(BigDecimal.valueOf(getByte(REGISTER_1_READ, register)));
    }

    private PercentType setPercentTypeRegister(Command cmd, byte[] register) throws IOException {
        if (cmd instanceof PercentType percentCommand) {
            byte value = (byte) ((percentCommand.intValue() + 5) / 10);
            set(REGISTER_1_WRITE, register, value);
        }
        return new PercentType(BigDecimal.valueOf(getByte(REGISTER_1_READ, register) * 10));
    }

    private OnOffType setOnOffTypeRegister(Command cmd, byte[] register) throws IOException {
        if (cmd instanceof OnOffType) {
            set(REGISTER_1_WRITE, register, OnOffType.ON.equals(cmd) ? (byte) 1 : (byte) 0);
        }
        return OnOffType.from(getBoolean(REGISTER_1_READ, register));
    }

    public OnOffType setBoost(Command cmd) throws IOException {
        return setOnOffTypeRegister(cmd, BOOST);
    }

    public OnOffType setNightCooling(Command cmd) throws IOException {
        return setOnOffTypeRegister(cmd, NIGHT_COOLING);
    }

    public OnOffType setBypass(Command cmd) throws IOException {
        return setOnOffTypeRegister(cmd, BYPASS);
    }
    */
}
