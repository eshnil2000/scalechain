package io.scalechain.blockchain.chain

import io.scalechain.blockchain.chain.mining.BlockTemplate
import io.scalechain.blockchain.proto.codec.TransactionCodec
import io.scalechain.blockchain.proto.{Transaction, CoinbaseData}
import io.scalechain.blockchain.storage.BlockStorage
import io.scalechain.blockchain.transaction.{CoinsView, CoinAmount, CoinAddress}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer


/**
  * Created by kangmo on 6/9/16.
  */
class BlockMining(transactionPool : TransactionPool, coinsView : CoinsView) {
  private val logger = LoggerFactory.getLogger(classOf[BlockMining])

  /*
    /** Calculate the (encoded) difficulty bits that should be in the block header.
      *
      * @param prevBlockDesc The descriptor of the previous block. This method calculates the difficulty of the next block of the previous block.
      * @return
      */
    def calculateDifficulty(prevBlockDesc : BlockInfo) : Long = {
      if (prevBlockDesc.height == 0) { // The genesis block
        GenesisBlock.BLOCK.header.target
      } else {
        // BUGBUG : Make sure that the difficulty calculation is same to the one in the Bitcoin reference implementation.
        val currentBlockHeight = prevBlockDesc.height + 1
        if (currentBlockHeight % 2016 == 0) {
          // TODO : Calculate the new difficulty bits.
          assert(false)
          -1L
        } else {
          prevBlockDesc.blockHeader.target
        }
      }
    }
  */

  /** Get the template for creating a block containing a list of transactions.
    *
    * @return The block template which has a sorted list of transactions to include into a block.
    */
  def getBlockTemplate(coinbaseData : CoinbaseData, minerAddress : CoinAddress, maxBlockSize : Int) : BlockTemplate = {
    // TODO : P1 - Use difficulty bits
    //val difficultyBits = getDifficulty()
    val difficultyBits = 10

    val validTransactions : List[Transaction] = transactionPool.getTransactionsFromPool().map {
      case (txHash, transaction) => transaction
    }

    val generationTranasction =
      TransactionBuilder.newBuilder(coinsView)
        .addGenerationInput(coinbaseData)
        .addOutput(CoinAmount(50), minerAddress)
        .build()

    // Select transactions by priority and fee. Also, sort them.
    val sortedTransactions = selectTransactions(generationTranasction, validTransactions, maxBlockSize)

    new BlockTemplate(difficultyBits, sortedTransactions)
  }


  /** Select transactions to include into a block.
    *
    *  Order transactions by fee in descending order.
    *  List N transactions based on the priority and fee so that the serialzied size of block
    *  does not exceed the max size. (ex> 1MB)
    *
    *  <Called by>
    *  When a miner tries to create a block, we have to create a block template first.
    *  The block template has the transactions to keep in the block.
    *  In the block template, it has all fields set except the nonce and the timestamp.
    *
    * @param transactions The candidate transactions
    * @param maxBlockSize The maximum block size. The serialized block size including the block header and transactions should not exceed the size.
    * @return The transactions to put into a block.
    */
  protected def selectTransactions(generationTransaction:Transaction, transactions : List[Transaction], maxBlockSize : Int) : List[Transaction] = {
    val selectedTransactions = new ListBuffer[Transaction]()
    // Step 1 : TODO : Select high priority transactions

    // Step 2 : TODO : Sort transactions by fee in descending order.

    // Step 3 : TODO : Choose transactions until we fill up the max block size.

    // Step 4 : TODO : Sort transactions based on a criteria to store into a block.

    // TODO : Need to check the sort order of transactions in a block.
    // TODO : Need to calculate the size of the block header on the fly instead of using the hard coded value 80.
    val BLOCK_HEADER_SIZE = 80
    val MAX_TRANSACTION_LENGTH_SIZE = 9 // The max size of variable int encoding.
    var serializedBlockSize = BLOCK_HEADER_SIZE + MAX_TRANSACTION_LENGTH_SIZE

    serializedBlockSize += TransactionCodec.serialize(generationTransaction).length
    selectedTransactions.append(generationTransaction)

    transactions foreach { tx =>
      serializedBlockSize += TransactionCodec.serialize(tx).length
      if (serializedBlockSize <= maxBlockSize) {
        selectedTransactions.append(tx)
      }
    }

    selectedTransactions.toList
  }
}
