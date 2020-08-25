package com.sunnyweather.android.logic

import androidx.lifecycle.liveData
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.network.SunnyWeatherNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.lang.Exception
import kotlin.RuntimeException
import kotlin.coroutines.CoroutineContext

object Repository {
    fun searchPlaces(query : String) = fire(Dispatchers.IO){
        val placeResponse = SunnyWeatherNetwork.searchPlaces(query)
        if(placeResponse.status == "ok") {
            val places = placeResponse.places
            Result.success(places)
        }else{
            Result.failure(RuntimeException("response status is ${placeResponse.status}"))
        }
    }

    fun refreshWeather (lng:String,lat:String) = fire(Dispatchers.IO){
        coroutineScope {
            val deferredRealtime = async {
                SunnyWeatherNetwork.getRealtimeWeather(lng,lat)
            }
            val deferredDailly = async {
                SunnyWeatherNetwork.getDailyWwather(lng,lat)
            }
            val realtimeResponse = deferredRealtime.await()
            val dailyResponse = deferredDailly.await()
            if(realtimeResponse.status == "ok" && dailyResponse.status == "ok"){
                val weather = Weather(realtimeResponse.result.realtime,dailyResponse.result.daily)
                Result.success(weather)
            }else{
                Result.failure(
                    RuntimeException("realtime resopnse status is ${realtimeResponse.status}" + "daily response status is ${dailyResponse.status}")
                )
            }
        }
    }


    private fun <T> fire(context : CoroutineContext, block: suspend () -> Result<T>) = liveData<Result<T>>(context) {
        val result = try {
            block()
        }catch (e:Exception){
            Result.failure<T>(e)
        }
       emit(result)
    }


}