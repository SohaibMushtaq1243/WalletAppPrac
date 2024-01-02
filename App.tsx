import './global';
import { StyleSheet, Text, View, Button } from 'react-native'
import React, { useEffect, useState } from 'react'
// import Web3 from 'web3';
import { ethers, utils, Wallet } from "ethers";
import 'react-native-get-random-values';
// import Web3 from 'web3';

const App = () => {
  // const Web3 = require('web3');
  const [Phrases, setPhrases] = useState<any>(null);
  const provider = new ethers.providers.JsonRpcProvider("https://mainnet.infura.io/v3/a0a5c874ec2b48cdb0e0a38304489275");
  async function generatePhrases() {
    const amount = ethers.Wallet.createRandom(16);
    console.log(amount)
    // const mnemonic = ethers.utils.entropyToMnemonic(randomBytes);
    // return mnemonic;
  }
  // const handleWeb3 = async () => {
  //   const web3 = new Web3(
  //     new Web3.providers.HttpProvider('https://mainnet.infura.io/v3/a0a5c874ec2b48cdb0e0a38304489275'),
  //   );

  //   console.log('web3====>>>', await web3.config.defaultChain);
  // };

  useEffect(() => {
    generatePhrases();
    // handleWeb3()
  }, [Phrases]);
  return (
    <View style={styles.container}>
      <Text style={styles.text}>{Phrases || ''}</Text>
      <Button title="Phrases" onPress={() => { generatePhrases() }} />
    </View>
  )
}

export default App

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center'
  },
  text: {
    color: 'white'
  }
})