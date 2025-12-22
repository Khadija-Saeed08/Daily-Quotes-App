package com.example.dailyquotes

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var quoteText: TextView
    private lateinit var authorText: TextView
    private lateinit var button: Button

    private lateinit var progressBar: ProgressBar

    private lateinit var favoriteButton : Button

    private lateinit var showFavButton: Button

    private lateinit var prefss: SharedPreferences

    private lateinit var shareButton: Button
    private val MAX_CACHE_SIZE = 20
    private val gson = Gson()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        quoteText = findViewById(R.id.quoteText)
        authorText = findViewById(R.id.authorText)
        button = findViewById(R.id.newQuoteButton)
        progressBar = findViewById(R.id.progressBar)
        favoriteButton = findViewById(R.id.favoriteButton)
        showFavButton = findViewById(R.id.showFavButton)
        shareButton = findViewById(R.id.shareButton)
        prefss = getSharedPreferences("quote_cache", MODE_PRIVATE)

        shareButton.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "${quoteText.text}\n${authorText.text}"
            )
            startActivity(Intent.createChooser(shareIntent, "Share Quote"))
        }


        val prefs = getSharedPreferences("favorites", MODE_PRIVATE)

        favoriteButton.setOnClickListener {

            val prefs = getSharedPreferences("favorites", MODE_PRIVATE)

            val currentFavorites =
                prefs.getStringSet("favorite_list", mutableSetOf())?.toMutableSet()

            val newFavorite = "${quoteText.text} ${authorText.text}"

            if (currentFavorites != null) {

                if (currentFavorites.contains(newFavorite)) {
                    Toast.makeText(this, "Already in favorites ❤️", Toast.LENGTH_SHORT).show()
                } else {
                    currentFavorites.add(newFavorite)

                    prefs.edit()
                        .putStringSet("favorite_list", currentFavorites)
                        .apply()

                    Toast.makeText(this, "Added to favorites ❤️", Toast.LENGTH_SHORT).show()
                }
            }
        }


        showFavButton.setOnClickListener {

            val prefs = getSharedPreferences("favorites", MODE_PRIVATE)
            val favorites = prefs.getStringSet("favorite_list", null)

            if (favorites.isNullOrEmpty()) {
                Toast.makeText(this, "No favorites saved yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val favoritesText = favorites.joinToString("\n\n")

            AlertDialog.Builder(this)
                .setTitle("❤️ Favorite Quotes")
                .setMessage(favoritesText)
                .setPositiveButton("OK", null)
                .show()
        }






        // Initial quote on app launch
        fetchQuote()

        // Fetch new quote on button click
        button.setOnClickListener {
            fetchQuote()
        }
    }


    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun fetchQuote() {

        if (!isInternetAvailable()) {
            // No internet, show cached quote immediately
            showCachedQuote()
            return
        }
        progressBar.visibility = View.VISIBLE
        quoteText.text = ""
        authorText.text = ""

        QuoteApiClient.instance.getRandomQuote()
            .enqueue(object : Callback<Quote> {

                override fun onResponse(call: Call<Quote>, response: Response<Quote>) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        val quote = response.body()!!
                        quoteText.text = "\"${quote.quote}\""
                        authorText.text = "— ${quote.author}"
                        prefss.edit()
                            .putString("cached_quote", quote.quote)
                            .putString("cached_author", quote.author)
                            .apply()
                        saveQuoteToCache(quote)
                    } else {
//                        quoteText.text = "Unable to load quote"
//                        authorText.text = "Please try again"
                        showCachedQuote()
                    }

                }

                override fun onFailure(call: Call<Quote>, t: Throwable) {
                    progressBar.visibility = View.GONE
//                    quoteText.text = "No internet connection"
//                    authorText.text = "Check your connection and try again"
                    showCachedQuote()
                }
            })
    }
    private fun showCachedQuote() {
        val cachedQuotesJson = prefss.getString("cached_quotes", null)
        if (cachedQuotesJson != null) {
            val type = object : TypeToken<List<Quote>>() {}.type
            val cachedQuotes: List<Quote> = gson.fromJson(cachedQuotesJson, type)

            if (cachedQuotes.isNotEmpty()) {
                // Pick random quote from cached list
                val randomQuote = cachedQuotes[Random.nextInt(cachedQuotes.size)]
                quoteText.text = "\"${randomQuote.quote}\""
                authorText.text = "— ${randomQuote.author}"

                Toast.makeText(
                    this,
                    "Showing offline cached quote",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        // No cached quotes
        quoteText.text = "No cached quote available"
        authorText.text = "Connect to the internet to load quotes"
    }


    private fun saveQuoteToCache(quote: Quote) {
        val cachedQuotesJson = prefss.getString("cached_quotes", null)
        val type = object : TypeToken<MutableList<Quote>>() {}.type
        val cachedQuotes: MutableList<Quote> = if (cachedQuotesJson != null) {
            gson.fromJson(cachedQuotesJson, type)
        } else {
            mutableListOf()
        }

        // Avoid duplicates
        if (!cachedQuotes.any { it.quote == quote.quote && it.author == quote.author }) {
            cachedQuotes.add(0, quote) // add new quote at the start

            // Keep only last 20 quotes
            if (cachedQuotes.size > MAX_CACHE_SIZE) {
                cachedQuotes.removeLast()
            }

            val newJson = gson.toJson(cachedQuotes)
            prefss.edit().putString("cached_quotes", newJson).apply()
        }
    }



}
