package com.example.appfinancetest.calculations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Atm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.ui.theme.*

fun getCategoryIconAndBg(item: TransactionDB): Pair<ImageVector, Color> {
    val cat = (item.category ?: "") + " " + (item.item ?: "") + " " + (item.label ?: "")
    val catLower = " " + cat.lowercase() + " "

    return when {
        // Bank Fees, Charges & Agios
        catLower.contains("frais") || catLower.contains("cotisation") || catLower.contains("agio") ||
                catLower.contains("commission") || catLower.contains("tenue de compte") ->
            Pair(Icons.Default.CreditCard, Color(0xFFDC2626))

        // Crypto & Bitcoin
        catLower.contains("btc") || catLower.contains("crypto") || catLower.contains("bitcoin") ||
                catLower.contains("binance") || catLower.contains("coinbase") || catLower.contains("kraken") ->
            Pair(Icons.Default.CurrencyBitcoin, CryptoAmber)

        // Bourse, Actions, ETF, PEA
        catLower.contains("bourse") || catLower.contains("etf") || catLower.contains("pea") ||
                catLower.contains("action") || catLower.contains("cto") || catLower.contains("trade") ->
            Pair(Icons.AutoMirrored.Filled.ShowChart, BourseBlue)

        // Savings & Savings Accounts
        catLower.contains("épargne") || catLower.contains("epargne") || catLower.contains("livret") ||
                catLower.contains("ldds") || catLower.contains("lep") || catLower.contains("placement") ->
            Pair(Icons.Default.Savings, LiquidityPurple)

        // Crowdfunding & Crowdlending
        catLower.contains("crowdfunding") || catLower.contains("crowdlending") || catLower.contains("clubfunding") ||
                catLower.contains("homunity") || catLower.contains("anaxago") ->
            Pair(Icons.Default.GroupWork, CrowdfundingViolet)

        // Real Estate, Rent & Property Management Fees
        catLower.contains("immobilier") || catLower.contains("loyer") || catLower.contains("syndic") ||
                catLower.contains("bail") || catLower.contains("copropriet") ->
            Pair(Icons.Default.HomeWork, EmeraldGreen)

        // Tolls & Highways
        catLower.contains("péage") || catLower.contains("peage") || catLower.contains("autoroute") ||
                catLower.contains("vinci") || catLower.contains("aprr") || catLower.contains("sanef") ||
                catLower.contains("area") ->
            Pair(Icons.Default.Toll, Color(0xFFF97316))

        // Donations, Gifts, Family Support & Parents
        catLower.contains("don") || catLower.contains("dons") || catLower.contains("donation") ||
                catLower.contains("cadeau") || catLower.contains("cadeaux") || catLower.contains("parent") ||
                catLower.contains("famille") || catLower.contains("argent de poche") || catLower.contains("association") ||
                catLower.contains("téléthon") || catLower.contains("croix rouge") ->
            Pair(Icons.Default.VolunteerActivism, Color(0xFFF43F5E))

        // Supermarket, Food & Groceries
        catLower.contains("supermarché") || catLower.contains("supermarche") || catLower.contains("courses") ||
                catLower.contains("alimentation") || catLower.contains("carrefour") || catLower.contains("auchan") ||
                catLower.contains("leclerc") || catLower.contains("lidl") || catLower.contains("monoprix") ||
                catLower.contains("intermarche") || catLower.contains("intermarché") || catLower.contains("casino") ||
                catLower.contains("franprix") || catLower.contains("picard") || catLower.contains("biocoop") ||
                catLower.contains("groceries") || catLower.contains("boulangerie") || catLower.contains("marché") || catLower.contains("marche") ->
            Pair(Icons.Default.ShoppingCart, Color(0xFF10B981))

        // Fast Food
        catLower.contains("mcdonald") || catLower.contains("mcdo") || catLower.contains("burger") ||
                catLower.contains("kfc") || catLower.contains("tacos") || catLower.contains("kebab") ||
                catLower.contains("subway") || catLower.contains("domino") ->
            Pair(Icons.Default.Fastfood, Color(0xFFF97316))

        // Restaurants, Cafés, Bars, Deliveroo & Uber Eats
        catLower.contains("restaurant") || catLower.contains("resto") || catLower.contains("pizza") ||
                catLower.contains("deliveroo") || catLower.contains("uber eats") || catLower.contains("eats") ||
                catLower.contains("sushi") || catLower.contains("starbucks") || catLower.contains("café") ||
                catLower.contains("cafe") || catLower.contains("bar") || catLower.contains("brasserie") ->
            Pair(Icons.Default.Restaurant, Color(0xFFEA580C))

        // Fuel, Gas & Gas Stations
        catLower.contains("essence") || catLower.contains("carburant") || catLower.contains("totalenergies") ||
                catLower.contains("station total") || catLower.contains("station bp") || catLower.contains("shell") ||
                catLower.contains("parking") || catLower.contains("garage") || catLower.contains("automobile") ->
            Pair(Icons.Default.LocalGasStation, Color(0xFFEF4444))

        // Airplane, Flights & Air France
        catLower.contains("avion") || catLower.contains("flight") || catLower.contains("air france") ||
                catLower.contains("easyjet") || catLower.contains("ryanair") ->
            Pair(Icons.Default.Flight, Color(0xFF0284C7))

        // Internet, Fiber, Broadband & Wi-Fi
        catLower.contains("internet") || catLower.contains("fibre") || catLower.contains("box") ||
                catLower.contains("wifi") || catLower.contains("freebox") || catLower.contains("bbox") ->
            Pair(Icons.Default.Wifi, Color(0xFF06B6D4))

        // Phone / Mobile / Plan
        catLower.contains("téléphone") || catLower.contains("telephone") || catLower.contains("mobile") ||
                catLower.contains("forfait") || catLower.contains("sosh") || catLower.contains("prixtel") ->
            Pair(Icons.Default.PhoneAndroid, Color(0xFF0284C7))

        // Train, Metro, Bus & Public Transport
        catLower.contains("train") || catLower.contains("sncf") || catLower.contains("ratp") ||
                catLower.contains(" tgv") || catLower.contains("tgv ") || catLower.contains(" ter ") ||
                catLower.contains("bus") || catLower.contains("metro") || catLower.contains("métro") ||
                catLower.contains("tram") || catLower.contains("transilien") || catLower.contains("transport") ||
                catLower.contains("taxi") || catLower.contains("uber") || catLower.contains("bolt") ->
            Pair(Icons.Default.Train, Color(0xFF2563EB))

        // Electricity, Water, Gas & Energy**
        catLower.contains("edf") || catLower.contains("engie") || catLower.contains("totalenergies") ||
                catLower.contains("électricité") || catLower.contains("electricite") || catLower.contains(" eau ") ||
                catLower.contains("veolia") || catLower.contains("suez") || catLower.contains("gaz") ||
                catLower.contains("energie") || catLower.contains("énergie") ->
            Pair(Icons.Default.Bolt, Color(0xFFEAB308))

        // Internet & General Telecommunications
        catLower.contains("orange") || catLower.contains("sfr") || catLower.contains("bouygues") ||
                catLower.contains("free") || catLower.contains("telecom") || catLower.contains("télécom") ->
            Pair(Icons.Default.Wifi, Color(0xFF06B6D4))

        // Abonnements & Streaming
        catLower.contains("netflix") || catLower.contains("spotify") || catLower.contains("disney") ||
                catLower.contains("canal") || catLower.contains("youtube") || catLower.contains("deezer") ||
                catLower.contains("prime video") || catLower.contains("apple") || catLower.contains("playstation") ||
                catLower.contains("xbox") || catLower.contains("nintendo") || catLower.contains("steam") ||
                catLower.contains("abonnement") || catLower.contains("subscription") ->
            Pair(Icons.Default.Subscriptions, Color(0xFFA855F7))

        // Shopping, Fashion, Electronics & Amazon
        catLower.contains("amazon") || catLower.contains("fnac") || catLower.contains("darty") ||
                catLower.contains("boulanger") || catLower.contains("zara") || catLower.contains("h&m") ||
                catLower.contains("decathlon") || catLower.contains("vetement") || catLower.contains("vêtement") ||
                catLower.contains("mode") || catLower.contains("shopping") || catLower.contains("sephora") ||
                catLower.contains("vinted") || catLower.contains("high-tech") || catLower.contains("tech") ->
            Pair(Icons.Default.ShoppingBag, Color(0xFFEC4899))

        // Healthcare, Pharmacy, Doctors & Doctolib
        catLower.contains("sante") || catLower.contains("santé") || catLower.contains("pharmacie") ||
                catLower.contains("medecin") || catLower.contains("médecin") || catLower.contains("docteur") ||
                catLower.contains("doctolib") || catLower.contains("hopital") || catLower.contains("hôpital") ||
                catLower.contains("dentiste") || catLower.contains("optique") || catLower.contains("soins") ->
            Pair(Icons.Default.MedicalServices, Color(0xFFF43F5E))

        // Sport, Fitness, Gym
        catLower.contains("sport") || catLower.contains("fitness") || catLower.contains("basic fit") ||
                catLower.contains("basic-fit") || catLower.contains("salle de sport") || catLower.contains("piscine") ->
            Pair(Icons.Default.FitnessCenter, Color(0xFF14B8A6))

        // Cinema, Films, Sorties, Spectacles
        catLower.contains("cinema") || catLower.contains("cinéma") || catLower.contains("theatre") ||
                catLower.contains("théâtre") || catLower.contains("concert") || catLower.contains("spectacle") ->
            Pair(Icons.Default.Movie, Color(0xFF8B5CF6))

        // Voyage, Hôtel, Booking, Airbnb
        catLower.contains("voyage") || catLower.contains("vacances") || catLower.contains("hotel") ||
                catLower.contains("hôtel") || catLower.contains("booking") || catLower.contains("airbnb") ||
                catLower.contains("sejour") || catLower.contains("séjour") ->
            Pair(Icons.Default.Hotel, Color(0xFF3B82F6))

        // Salaire, Revenu, Paie, Pension
        catLower.contains("salaire") || catLower.contains("paie") || catLower.contains("pension") ||
                catLower.contains("revenu") || catLower.contains("revenus") || catLower.contains("allocation") ||
                catLower.contains("caf") || catLower.contains("prime") ->
            Pair(Icons.Default.Payments, Color(0xFF22C55E))

        // Virement, Remboursement, Transfert, Paypal, Lydia
        catLower.contains("virement") || catLower.contains("transfert") || catLower.contains("remboursement") ||
                catLower.contains("paypal") || catLower.contains("lydia") || catLower.contains("paylib") ||
                catLower.contains("revolut") || catLower.contains("n26") ->
            Pair(Icons.AutoMirrored.Filled.CompareArrows, TealDark)

        // Retrait DAB / Espèces
        catLower.contains("retrait") || catLower.contains("dab") || catLower.contains("especes") ||
                catLower.contains("espèces") || catLower.contains("cash") || catLower.contains("distributeur") ->
            Pair(Icons.Default.Atm, Color(0xFF64748B))

        // Impôts, Taxes, Service Public, Amende
        catLower.contains("impot") || catLower.contains("impôt") || catLower.contains("taxe") ||
                catLower.contains("amende") || catLower.contains("tresor public") || catLower.contains("trésor public") ||
                catLower.contains("urssaf") ->
            Pair(Icons.AutoMirrored.Filled.ReceiptLong, Color(0xFF475569))

        // Assurance, Mutuelle
        catLower.contains("assurance") || catLower.contains("mutuelle") || catLower.contains("maaf") ||
                catLower.contains("macif") || catLower.contains("mma") || catLower.contains("allianz") ||
                catLower.contains("axa") || catLower.contains("alan") ->
            Pair(Icons.Default.Shield, Color(0xFF6366F1))

        // Animaux & Vétérinaire
        catLower.contains("animal") || catLower.contains("animaux") || catLower.contains("chien") ||
                catLower.contains("chat") || catLower.contains("veterinaire") || catLower.contains("vétérinaire") ->
            Pair(Icons.Default.Pets, Color(0xFFA16207))

        // Enfants, Crèche, École, Jouets
        catLower.contains("enfant") || catLower.contains("creche") || catLower.contains("crèche") ||
                catLower.contains("ecole") || catLower.contains("école") || catLower.contains("scolaire") ||
                catLower.contains("bebe") || catLower.contains("bébé") || catLower.contains("jouet") ->
            Pair(Icons.Default.ChildCare, Color(0xFFD946EF))

        // Defaut / Inconnu
        else -> Pair(Icons.Default.AccountBalanceWallet, IncomeBlue)
    }
}
