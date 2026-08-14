package com.shieldcore.security.nativeengine

import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-performance JNI bridge for the antivirus scanning engine.
 * Handles hash calculations and binary signature matching in native code.
 */
@Singleton
class NativeScannerBridge @Inject constructor() {

    init {
        System.loadLibrary("shieldcore_native")
    }

    /**
     * Computes a high-performance FNV-1a hash of the file at the given path.
     * @param filePath Absolute path to the file.
     * @return Hex string of the hash or an error code.
     */
    external fun computeFileHash(filePath: String): String

    /**
     * Scans a file for specific binary/string signature patterns.
     * @param filePath Absolute path to the file.
     * @param signaturePatterns Array of hex or string signatures to search for.
     * @return True if any pattern matches.
     */
    external fun scanBinarySignatures(filePath: String, signaturePatterns: Array<String>): Boolean
}
