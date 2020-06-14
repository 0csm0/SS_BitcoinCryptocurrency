import java.net.URI;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;

public class TxHandler {
    private UTXOPool _utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is {@code utxoPool}. This should make a copy of utxoPool
     * by using the UTXOPool(UTXOPool uPool) constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this._utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if: (1) all outputs claimed by {@code tx} are in the current
     *         UTXO pool, (2) the signatures on each input of {@code tx} are valid,
     *         (3) no UTXO is claimed multiple times by {@code tx}, (4) all of
     *         {@code tx}s output values are non-negative, and (5) the sum of
     *         {@code tx}s input values is greater than or equal to the sum of its
     *         output values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        ArrayList<UTXO> usedUTXO = new ArrayList<>();
        int sumOfInputs = 0; // for (5)
        int sumOfOutputs = 0; // for (5)

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            // (1) all outputs claimed by {@code tx} are in the current UTXO pool
            if (!_utxoPool.contains(utxo))
                return false;

            // (2) the signatures on each input of {@code tx} are valid,
            Transaction.Output previouOutput = this._utxoPool.getTxOutput(utxo);
            PublicKey pubKey = previouOutput.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = input.signature;
            if (!Crypto.verifySignature(pubKey, message, signature))
                return false;

            // (3) no UTXO is claimed multiple times by {@code tx}
            if (usedUTXO.contains(utxo))
                return false;
            usedUTXO.add(utxo);

            sumOfInputs += previouOutput.value;

        }

        for (int i = 0; i < tx.numOutputs(); i++) {
            // (4) all of {@code tx}s output values are non-negative
            Transaction.Output output = tx.getOutput(i);
            if (output.value < 0)
                return false;
            sumOfOutputs += output.value;
        }

        // (5) the sum of {@code tx}s input values is greater than or equal to the sum
        // of its output values
        if (sumOfInputs < sumOfOutputs)
            return false;
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions,
     * checking each transaction for correctness, returning a mutually valid array
     * of accepted transactions, and updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                validTxs.add(tx);
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    this._utxoPool.removeUTXO(utxo);
                }
                int index = 0;
                for (Transaction.Output output : tx.getOutputs()) {
                    UTXO utxo = new UTXO(tx.getHash(), index);
                    this._utxoPool.addUTXO(utxo, output);
                    index++;
                }
            }
        }
        return validTxs.toArray(new Transaction[validTxs.size()]);
    }

}
