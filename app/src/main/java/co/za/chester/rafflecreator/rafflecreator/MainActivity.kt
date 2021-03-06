package co.za.chester.rafflecreator.rafflecreator

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import co.za.chester.rafflecreator.rafflecreator.domain.Participant
import co.za.chester.rafflecreator.rafflecreator.domain.Raffle
import co.za.chester.rafflecreator.rafflecreator.domain.Repository
import org.funktionale.option.getOrElse


class MainActivity : AppCompatActivity() {

    private var exitCounter: Int = 0
    private lateinit var raffleRecyclerView: RecyclerView
    private lateinit var raffles: ArrayList<Raffle>
    private lateinit var customRecyclerViewAdapter: CustomRecyclerViewAdapter<Raffle>
    private lateinit var raffleRepository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        resetExitCounter()
        raffleRecyclerView = findViewById(R.id.raffleRecyclerView)
        raffleRepository = Repository(this, getString(R.string.raffle_key))
        raffles = java.util.ArrayList()
        customRecyclerViewAdapter = CustomRecyclerViewAdapter(raffles, { raffle, customViewHolder ->
            customViewHolder.label.text = raffle.name
        }, removeRaffleAction(), { position ->
            val raffle = raffles[position]
            openParticipantActivity(raffle)
        })
        val layoutManager = LinearLayoutManager(applicationContext)
        raffleRecyclerView.layoutManager = layoutManager
        raffleRecyclerView.itemAnimator = DefaultItemAnimator()
        raffleRecyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        raffleRecyclerView.adapter = customRecyclerViewAdapter
        populateRaffleList()

        if (raffles.isEmpty()) {
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder
                    .setMessage("No raffles added, please press + button below to add a raffle")
                    .setPositiveButton("OK", { dialog, _ ->
                        dialog.dismiss()
                    })
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }
    }

    private fun removeRaffleAction(): (ArrayList<Raffle>, Int, RecyclerView.Adapter<CustomViewHolder>) -> Unit {
        return { values, position, adapter ->
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder
                    .setCancelable(false)
                    .setMessage("Are you sure you want to remove this Raffle: ${values[position].name}")
                    .setPositiveButton("Yes", { dialog, _ ->
                        val maybeParticipantData = raffleRepository.readString(getString(R.string.participant_key))
                        val participants = maybeParticipantData.map { participant ->
                            ArrayList(Participant.toObjects(participant))
                        }.getOrElse {
                            ArrayList()
                        }
                        val raffle = raffles[position]
                        val isParticipantsRemoved = participants.removeAll { p -> p.raffleId == raffle.id }
                        raffles.removeAt(position)
                        if (isParticipantsRemoved) {
                            raffleRepository.saveString(getString(R.string.participant_key), Participant.fromObjects(participants))
                        }
                        raffleRepository.saveString(getString(R.string.raffle_name_key), Raffle.fromObjects(raffles))
                        adapter.notifyDataSetChanged()
                        dialog.dismiss()
                    })
                    .setNegativeButton("No",
                            { dialog, _ ->
                                dialog.cancel()
                            })

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }
    }

    override fun onPause() {
        raffleRepository.saveString(getString(R.string.raffle_name_key), Raffle.fromObjects(raffles))
        super.onPause()
    }

    private fun populateRaffleList() {
        val maybeRaffleData = raffleRepository.readString(getString(R.string.raffle_name_key))
        raffles.addAll(maybeRaffleData.map { raffleData ->
            Raffle.toObjects(raffleData)
        }.getOrElse {
            ArrayList()
        })
        customRecyclerViewAdapter.notifyDataSetChanged()
    }

    private fun resetExitCounter() {
        exitCounter = 2
    }

    override fun onBackPressed() {
        --exitCounter
        when (exitCounter) {
            1 -> Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_LONG).show()
            0 -> this.finish()
        }
    }

    fun addRaffle(view: View) {
        resetExitCounter()
        openRaffleNameDialog()
    }

    private fun openRaffleNameDialog() {
        val layoutInflater = LayoutInflater.from(this)
        val promptView = layoutInflater.inflate(R.layout.prompt, null)
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(promptView)

        val raffleNameInput = promptView.findViewById(R.id.editTextDialogRaffleNameInput) as EditText
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Add", { dialog, _ ->
                    val raffleName = raffleNameInput.text.toString()
                    if (!raffleName.isEmpty()) {
                        Toast.makeText(this, "$raffleName added", Toast.LENGTH_LONG).show()
                        val raffle = Raffle(raffleName)
                        raffles.add(raffle)
                        raffleRepository.saveString(getString(R.string.raffle_name_key), Raffle.fromObjects(raffles))
                        customRecyclerViewAdapter.notifyDataSetChanged()
                        openParticipantActivity(raffle)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "No Raffle Name entered", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    }
                })
                .setNegativeButton("Cancel",
                        { dialog, _ ->
                            dialog.cancel()
                        })

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun openParticipantActivity(raffle: Raffle) {
        val raffleIntent = Intent(this, RaffleActivity::class.java)
        raffleIntent.putExtra(RaffleActivity.RAFFLE_NAME, raffle.name)
        raffleIntent.putExtra(RaffleActivity.RAFFLE_ID, raffle.id.toString())
        startActivity(raffleIntent)
    }
}
