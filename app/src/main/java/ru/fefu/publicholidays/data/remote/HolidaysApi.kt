package ru.fefu.publicholidays.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import ru.fefu.publicholidays.data.model.PublicHolidayDto

interface HolidaysApi {

    @GET("api/v3/PublicHolidays/{year}/{countryCode}")
    suspend fun getPublicHolidays(
        @Path("year") year: Int,
        @Path("countryCode") countryCode: String
    ): List<PublicHolidayDto>
}