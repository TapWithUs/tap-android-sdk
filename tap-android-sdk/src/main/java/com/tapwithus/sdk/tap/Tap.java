package com.tapwithus.sdk.tap;

public class Tap {

    private final String identifier;
    private final String name;
    private final int battery;
    private final String serialNumber;
    private final String hwVer;
    private final String fwVer;

    public Tap(String identifier, String name, int battery, String serialNumber, String hwVer, String fwVer) {
        this.identifier = identifier;
        this.name = name;
        this.battery = battery;
        this.serialNumber = serialNumber;
        this.hwVer = hwVer;
        this.fwVer = fwVer;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public int getBattery() {
        return battery;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getHwVer() {
        return hwVer;
    }

    public String getFwVer() {
        return fwVer;
    }

    @Override
    public String toString() {
        return "Tap{" +
                "identifier='" + identifier + '\'' +
                ", name='" + name + '\'' +
                ", battery=" + battery +
                ", serialNumber='" + serialNumber + '\'' +
                ", hwVer='" + hwVer + '\'' +
                ", fwVer='" + fwVer + '\'' +
                '}';
    }
}
