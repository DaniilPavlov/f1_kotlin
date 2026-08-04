package com.example.f1_kotlin.data.mapper

import com.example.f1_kotlin.data.model.CircuitLocationModel
import com.example.f1_kotlin.data.model.CircuitModel
import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.data.model.DriverStandingsModel
import com.example.f1_kotlin.data.model.RaceModel
import com.example.f1_kotlin.data.model.RaceResultModel
import com.example.f1_kotlin.data.model.TimeModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JolpicaMappersTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test
    fun driverModel_missingOptionalBioFields_parses() {
        val json = """
            {"driverId":"paul_aron","givenName":"Paul","familyName":"Aron"}
        """.trimIndent()
        val adapter = moshi.adapter(DriverModel::class.java)
        val dto = adapter.fromJson(json)!!
        assertEquals("paul_aron", dto.driverId)
        assertEquals("", dto.url)
        assertEquals("", dto.dateOfBirth)
        assertEquals("", dto.nationality)
        assertEquals("Paul Aron", dto.toDomain().fullName)
    }

    @Test
    fun driverStandingsModel_toDomain_mapsFields() {
        val dto = DriverStandingsModel(
            position = "1",
            positionText = "1",
            points = "100",
            wins = "5",
            driver = DriverModel(
                driverId = "verstappen",
                url = "https://example.com/max",
                givenName = "Max",
                familyName = "Verstappen",
                dateOfBirth = "1997-09-30",
                nationality = "Dutch",
                code = "VER",
                permanentNumber = "1",
            ),
            constructors = listOf(
                ConstructorModel("red_bull", "https://example.com/rb", "Red Bull", "Austrian"),
            ),
        )

        val domain = dto.toDomain()

        assertEquals("1", domain.position)
        assertEquals("100", domain.points)
        assertEquals("5", domain.wins)
        assertEquals("verstappen", domain.driver.driverId)
        assertEquals("Max Verstappen", domain.driver.fullName)
        assertEquals("VER", domain.driver.code)
        assertEquals("1", domain.driver.permanentNumber)
        assertEquals(1, domain.constructors.size)
        assertEquals("red_bull", domain.constructors.first().constructorId)
        assertEquals("Red Bull", domain.constructors.first().name)
    }

    @Test
    fun driverStandingsModel_missingPosition_defaultsEmpty() {
        // Jolpica R1 2026: Stroll без "position", только positionText="-".
        val dto = DriverStandingsModel(
            positionText = "-",
            points = "0",
            wins = "0",
            driver = DriverModel(
                driverId = "stroll",
                url = "",
                givenName = "Lance",
                familyName = "Stroll",
                dateOfBirth = "1998-10-29",
                nationality = "Canadian",
                code = "STR",
                permanentNumber = "18",
            ),
            constructors = listOf(
                ConstructorModel("aston_martin", "", "Aston Martin", "British"),
            ),
        )
        val domain = dto.toDomain()
        assertEquals("", domain.position)
        assertEquals("-", domain.positionText)
        assertEquals("stroll", domain.driver.driverId)
    }

    @Test
    fun moshi_parsesDriverStandingsWithoutPositionField() {
        val json = """
            {
              "positionText": "-",
              "points": "0",
              "wins": "0",
              "Driver": {
                "driverId": "stroll",
                "url": "",
                "givenName": "Lance",
                "familyName": "Stroll",
                "dateOfBirth": "1998-10-29",
                "nationality": "Canadian",
                "code": "STR",
                "permanentNumber": "18"
              },
              "Constructors": [
                {
                  "constructorId": "aston_martin",
                  "url": "",
                  "name": "Aston Martin",
                  "nationality": "British"
                }
              ]
            }
        """.trimIndent()

        val parsed = moshi.adapter(DriverStandingsModel::class.java).fromJson(json)!!
        assertEquals("", parsed.position)
        assertEquals("-", parsed.positionText)
        assertEquals("stroll", parsed.driver.driverId)
    }

    @Test
    fun raceModel_toDomain_mapsCircuitAndResults() {
        val dto = RaceModel(
            season = "2026",
            round = "8",
            url = "https://example.com/monaco",
            raceName = "Monaco Grand Prix",
            circuit = CircuitModel(
                circuitId = "monaco",
                url = "https://example.com/circuit",
                circuitName = "Circuit de Monaco",
                location = CircuitLocationModel("43.7", "7.4", "Monte-Carlo", "Monaco"),
            ),
            date = "2026-05-24",
            time = "13:00:00Z",
            results = listOf(
                RaceResultModel(
                    number = "1",
                    position = "1",
                    positionText = "1",
                    points = "25",
                    driver = DriverModel(
                        driverId = "leclerc",
                        url = "",
                        givenName = "Charles",
                        familyName = "Leclerc",
                        dateOfBirth = "1997-10-16",
                        nationality = "Monegasque",
                    ),
                    constructor = ConstructorModel("ferrari", "", "Ferrari", "Italian"),
                    grid = "1",
                    laps = "78",
                    status = "Finished",
                    time = TimeModel(millis = "7200000", time = "2:00:00.000"),
                    fastestLap = null,
                ),
            ),
        )

        val domain = dto.toDomain()

        assertEquals("2026", domain.season)
        assertEquals("8", domain.round)
        assertEquals("Monaco Grand Prix", domain.raceName)
        assertEquals("monaco", domain.circuit.circuitId)
        assertEquals("Monte-Carlo", domain.circuit.location.locality)
        assertEquals("13:00:00Z", domain.time)
        assertEquals(1, domain.results?.size)
        val result = domain.results!!.first()
        assertEquals("leclerc", result.driver.driverId)
        assertEquals("Ferrari", result.constructor.name)
        assertEquals("2:00:00.000", result.time?.time)
        assertNull(result.fastestLap)
        assertNull(domain.qualifyingResults)
        assertNull(domain.pitStops)
    }
}
