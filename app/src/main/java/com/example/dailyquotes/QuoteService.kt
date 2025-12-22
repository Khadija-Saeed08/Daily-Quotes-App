package com.example.dailyquotes
import retrofit2.Call
import retrofit2.http.GET

interface QuoteService {
    @GET("quotes/random")
    fun getRandomQuote(): Call<Quote>
}
