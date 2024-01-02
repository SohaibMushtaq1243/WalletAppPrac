package com.walletappp

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.google.gson.Gson
import wallet.core.jni.*
import java.security.MessageDigest
import kotlin.experimental.and


data class WalletData (
    var hd_path     : String = "",
    var seed       : String = "",
    var public_key  : String = "",
    var private_key : String = "",
    var wif        : String = "",
    var address    : String = "",
    var index      : Int = 0,
    var coinSymbol : String
)

class WalletModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    private val logTag = "WalletModule"


    override fun getName(): String {
        return "WalletModule"
    }

    @ReactMethod
    fun mnemonicToSeed(seedPhrase: String,promise: Promise) {
        promise.resolve(HDWallet(seedPhrase,"").seed().toHexString(false))
    }

    @ReactMethod
    fun generateMnemonic(promise: Promise) {
        val wallet = HDWallet(128, "")
        promise.resolve(wallet.mnemonic())
    }

    @ReactMethod
    fun isMnemonicValid(mnemonic:String,promise: Promise) {
        promise.resolve(Mnemonic.isValid(mnemonic))
    }

    @ReactMethod
    fun createSingleWallet(seedPhrase:String, coinSymbol: String, hdPath:String, promise: Promise){
        val hdWallet = HDWallet(seedPhrase, "")
        val wallet = WalletData(coinSymbol = coinSymbol,hd_path = hdPath)
        val generatedWallet = generateWallet(hdWallet,wallet)

        val gson = Gson()
        val jsonString = gson.toJson(generatedWallet)
        promise.resolve(jsonString)
    }


    fun generateWallet(hdWallet: HDWallet,walletData: WalletData):WalletData {
        val coinType = getCoinType(walletData.coinSymbol)
        val key = hdWallet.getKey(coinType,walletData.hd_path)

        if(walletData.coinSymbol == "btc") walletData.wif = privateKeyToWif(key.data().toHexString(false),"80")
        else if (walletData.coinSymbol == "doge") walletData.wif = privateKeyToWif(key.data().toHexString(false),"9E")

        if(walletData.coinSymbol == "eth"){
            walletData.address = coinType.deriveAddress(key).toLowerCase()
        } else {
            walletData.address = coinType.deriveAddress(key)
        }

        walletData.private_key = key.data().toHexString(false)
        walletData.public_key = key.getPublicKeySecp256k1(true).data().toHexString(false) // Works for BTC

        return walletData
    }

    @ReactMethod
    fun createInitialWallets(seedPhrase:String,promise: Promise){
        val btcWallet = WalletData(coinSymbol = "btc",hd_path = "m/84'/0'/0'/0/0")
        val ethWallet = WalletData(coinSymbol = "eth",hd_path = "m/44'/60'/0'/0/0")
        val bnbWallet :WalletData
        val egcWallet :WalletData
        val dogeWallet = WalletData(coinSymbol = "doge",hd_path = "m/44'/3'/0'/0/0")
        val tronWallet = WalletData(coinSymbol = "trx",hd_path = "m/44'/195'/0'/0/0")

        val wallets = mutableListOf<WalletData>()
        val wallet = HDWallet(seedPhrase, "")

        //BTC Wallet
        createWallet(wallet,btcWallet,wallets)
        //TRON Wallet
        createWallet(wallet,tronWallet,wallets)
        //Doge Wallet
        createWallet(wallet,dogeWallet,wallets)
        //ETH and BNB Wallet
        createWallet(wallet,ethWallet,wallets)
        bnbWallet = ethWallet.copy(public_key = ethWallet.public_key,private_key = ethWallet.private_key,address = ethWallet.address,coinSymbol = "bnb")
        wallets.add(bnbWallet)
        egcWallet = ethWallet.copy(public_key = ethWallet.public_key,private_key = ethWallet.private_key,address = ethWallet.address,coinSymbol = "egc")
        wallets.add(egcWallet)
        val gson = Gson()
        val jsonString = gson.toJson(wallets)
//        Log.d(logTag, "BNB Wallet Check:$jsonString")
        promise.resolve(jsonString)
    }

    private fun createWallet(wallet:HDWallet,walletData:WalletData,wallets: MutableList<WalletData>) {
        val coinType = getCoinType(walletData.coinSymbol)
        val key = wallet.getKey(coinType,walletData.hd_path)

        if(walletData.coinSymbol == "eth"){
            walletData.address = coinType.deriveAddress(key).toLowerCase()
        } else {
            walletData.address = coinType.deriveAddress(key)
        }
        // val withPrefix = walletData.coinSymbol == "eth"
        walletData.private_key = key.data().toHexString(false)
        walletData.public_key = key.getPublicKeySecp256k1(true).data().toHexString(false) // Works for BTC

        if(walletData.coinSymbol == "btc") {
            walletData.wif = privateKeyToWif(key.data().toHexString(false),"80")
            walletData.private_key = privateKeyToWif(key.data().toHexString(false),"80")
        }
        else if (walletData.coinSymbol == "doge") walletData.wif = privateKeyToWif(key.data().toHexString(false),"9E")

        //Adding to Wallets list
        wallets.add(walletData)
    }

    private fun ByteArray.toHexString(withPrefix: Boolean = true): String {
        val stringBuilder = StringBuilder()
        if(withPrefix) {
            stringBuilder.append("0x")
        }
        for (element in this) {
            stringBuilder.append(String.format("%02x", element and 0xFF.toByte()))
        }
        return stringBuilder.toString()
    }

    private fun String.hexStringToByteArray() : ByteArray {
        val HEX_CHARS = "0123456789ABCDEF"
        val result = ByteArray(length / 2)
        for (i in 0 until length step 2) {
            val firstIndex = HEX_CHARS.indexOf(this[i].toUpperCase())
            val secondIndex = HEX_CHARS.indexOf(this[i + 1].toUpperCase())
            val octet = firstIndex.shl(4).or(secondIndex)
            result.set(i.shr(1), octet.toByte())
        }
        return result
    }

    private fun privateKeyToWif(privateKey:String,prefix: String):String {
        val step1 = prefix+privateKey+"01"
        val step2ByteArray = MessageDigest.getInstance("SHA-256").digest(step1.hexStringToByteArray())
        val step3ByteArray = MessageDigest.getInstance("SHA-256").digest(step2ByteArray)
        val step4Checksum = step3ByteArray.toHexString(false).take(8)
        val step5AppendChecksum = step1 + step4Checksum
        return Base58.encodeNoCheck(step5AppendChecksum.hexStringToByteArray())
    }

    private fun getCoinType(coinSymbol: String):CoinType {
        if (coinSymbol == "btc") return CoinType.BITCOIN
        else if(coinSymbol == "trx") return CoinType.TRON
        else if(coinSymbol == "doge") return CoinType.DOGECOIN
        else return CoinType.ETHEREUM
    }

}
