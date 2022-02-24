package com.aliucord.manager

import java.security.PrivateKey
import java.security.cert.X509Certificate

class KeySet(val publicKey: X509Certificate, val privateKey: PrivateKey)