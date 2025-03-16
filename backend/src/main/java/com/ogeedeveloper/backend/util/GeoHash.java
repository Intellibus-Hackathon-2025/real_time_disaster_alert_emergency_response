package com.ogeedeveloper.backend.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for encoding and decoding GeoHash values.
 * GeoHash is a public domain geocoding system that encodes geographic coordinates
 * into a short string of letters and digits.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GeoHash {
    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    /**
     * Encodes a latitude/longitude point to a GeoHash string.
     *
     * @param latitude the latitude coordinate
     * @param longitude the longitude coordinate
     * @param precision the desired length of the returned GeoHash
     * @return the GeoHash string
     */
    public static String encodeHash(double latitude, double longitude, int precision) {
        if (precision < 1) {
            return "";
        }

        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};

        StringBuilder hash = new StringBuilder();
        boolean isEven = true;
        int bit = 0;
        int charIndex = 0;

        while (hash.length() < precision) {
            if (isEven) {
                double mid = (lonRange[0] + lonRange[1]) / 2;
                if (longitude > mid) {
                    charIndex = (charIndex << 1) + 1;
                    lonRange[0] = mid;
                } else {
                    charIndex = (charIndex << 1);
                    lonRange[1] = mid;
                }
            } else {
                double mid = (latRange[0] + latRange[1]) / 2;
                if (latitude > mid) {
                    charIndex = (charIndex << 1) + 1;
                    latRange[0] = mid;
                } else {
                    charIndex = (charIndex << 1);
                    latRange[1] = mid;
                }
            }

            isEven = !isEven;

            if (++bit == 5) {
                hash.append(BASE32.charAt(charIndex));
                bit = 0;
                charIndex = 0;
            }
        }

        return hash.toString();
    }

    /**
     * Decodes a GeoHash string into a latitude/longitude point.
     *
     * @param geohash the GeoHash string to decode
     * @return a double array with [latitude, longitude]
     */
    public static double[] decodeHash(String geohash) {
        if (geohash == null || geohash.isEmpty()) {
            return new double[] {0, 0};
        }

        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};

        boolean isEven = true;

        for (int i = 0; i < geohash.length(); i++) {
            char c = geohash.charAt(i);
            int cd = BASE32.indexOf(c);

            for (int j = 4; j >= 0; j--) {
                int mask = 1 << j;
                if (isEven) {
                    if ((cd & mask) != 0) {
                        lonRange[0] = (lonRange[0] + lonRange[1]) / 2;
                    } else {
                        lonRange[1] = (lonRange[0] + lonRange[1]) / 2;
                    }
                } else {
                    if ((cd & mask) != 0) {
                        latRange[0] = (latRange[0] + latRange[1]) / 2;
                    } else {
                        latRange[1] = (latRange[0] + latRange[1]) / 2;
                    }
                }
                isEven = !isEven;
            }
        }

        double latitude = (latRange[0] + latRange[1]) / 2;
        double longitude = (lonRange[0] + lonRange[1]) / 2;

        return new double[] {latitude, longitude};
    }

    /**
     * Checks if a location is within the area defined by a GeoHash prefix.
     *
     * @param latitude the latitude to check
     * @param longitude the longitude to check
     * @param geohashPrefix the GeoHash prefix defining an area
     * @return true if the location is within the GeoHash area
     */
    public static boolean isInGeoHashArea(double latitude, double longitude, String geohashPrefix) {
        if (geohashPrefix == null || geohashPrefix.isEmpty()) {
            return false;
        }

        String locationHash = encodeHash(latitude, longitude, geohashPrefix.length());
        return locationHash.startsWith(geohashPrefix);
    }

    /**
     * Gets the approximate error (in meters) for a given GeoHash length.
     *
     * @param precision the length of the GeoHash
     * @return the approximate error in meters
     */
    public static double getError(int precision) {
        switch (precision) {
            case 1: return 2500000; // ±2500 km
            case 2: return 630000;  // ±630 km
            case 3: return 78000;   // ±78 km
            case 4: return 20000;   // ±20 km
            case 5: return 2400;    // ±2.4 km
            case 6: return 610;     // ±0.61 km
            case 7: return 76;      // ±76 m
            case 8: return 19;      // ±19 m
            case 9: return 2.4;     // ±2.4 m
            case 10: return 0.6;    // ±0.6 m
            case 11: return 0.074;  // ±7.4 cm
            case 12: return 0.019;  // ±1.9 cm
            default:
                if (precision < 1) return Double.MAX_VALUE;
                if (precision > 12) return 0.019;
                return Double.MAX_VALUE;
        }
    }
}
