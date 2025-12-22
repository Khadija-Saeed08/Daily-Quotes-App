package com.example.dailyquotes
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object QuoteApiClient {

    val instance: QuoteService by lazy {

        val retrofit = Retrofit.Builder()
            .baseUrl("https://dummyjson.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(QuoteService::class.java)
    }
}
