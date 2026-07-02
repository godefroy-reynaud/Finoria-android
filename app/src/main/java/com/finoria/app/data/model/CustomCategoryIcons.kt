package com.finoria.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.CrueltyFree
import androidx.compose.material.icons.outlined.CurrencyPound
import androidx.compose.material.icons.outlined.CurrencyYen
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.EmojiNature
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PedalBike
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tram
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WineBar
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Banque des ~72 symboles proposés pour les catégories personnalisées.
 *
 * Remap du jeu de SF Symbols iOS du portage vers Material Icons. La **clé** (nom
 * snake_case, stable) est la valeur stockée en base dans `CustomCategory.symbol` —
 * ne jamais renommer une clé publiée. L'ordre d'insertion est l'ordre d'affichage
 * dans la grille de la sheet (6 colonnes).
 */
object CustomCategoryIcons {

    val all: Map<String, ImageVector> = linkedMapOf(
        // Divers / repli
        "sell" to Icons.Outlined.Sell,                              // tag.fill
        "help" to Icons.AutoMirrored.Outlined.Help,                 // questionmark.circle.fill
        // Courses & repas
        "shopping_cart" to Icons.Outlined.ShoppingCart,             // cart.fill
        "shopping_basket" to Icons.Outlined.ShoppingBasket,         // basket.fill
        "restaurant" to Icons.Outlined.Restaurant,                  // fork.knife
        "local_cafe" to Icons.Outlined.LocalCafe,                   // cup.and.saucer.fill
        "fastfood" to Icons.Outlined.Fastfood,                      // takeoutbag.and.cup...
        "cake" to Icons.Outlined.Cake,                              // birthday.cake.fill
        "wine_bar" to Icons.Outlined.WineBar,                       // wineglass.fill
        // Logement & énergie
        "home" to Icons.Outlined.Home,                              // house.fill
        "apartment" to Icons.Outlined.Apartment,                    // building.2.fill
        "bed" to Icons.Outlined.Bed,                                // bed.double.fill
        "bolt" to Icons.Outlined.Bolt,                              // bolt.fill
        "water_drop" to Icons.Outlined.WaterDrop,                   // drop.fill
        "local_fire_department" to Icons.Outlined.LocalFireDepartment, // flame.fill
        "wifi" to Icons.Outlined.Wifi,                              // wifi
        "phone" to Icons.Outlined.Phone,                            // phone.fill
        // Transport
        "directions_car" to Icons.Outlined.DirectionsCar,           // car.fill
        "directions_bus" to Icons.Outlined.DirectionsBus,           // bus.fill
        "pedal_bike" to Icons.Outlined.PedalBike,                   // bicycle
        "tram" to Icons.Outlined.Tram,                              // tram.fill
        "local_gas_station" to Icons.Outlined.LocalGasStation,      // fuelpump.fill
        "flight" to Icons.Outlined.Flight,                          // airplane
        // Argent
        "payments" to Icons.Outlined.Payments,                      // banknote.fill
        "credit_card" to Icons.Outlined.CreditCard,                 // creditcard.fill
        "euro" to Icons.Outlined.Euro,                              // eurosign.circle.fill
        "attach_money" to Icons.Outlined.AttachMoney,               // dollarsign.circle.fill
        "currency_pound" to Icons.Outlined.CurrencyPound,           // sterlingsign.circle.fill
        "currency_yen" to Icons.Outlined.CurrencyYen,               // yensign.circle.fill
        "show_chart" to Icons.AutoMirrored.Outlined.ShowChart,      // chart.line.uptrend.xyaxis
        "bar_chart" to Icons.Outlined.BarChart,                     // chart.bar.fill
        "pie_chart" to Icons.Outlined.PieChart,                     // chart.pie.fill
        // Travail & documents
        "business_center" to Icons.Outlined.BusinessCenter,         // briefcase.fill
        "description" to Icons.Outlined.Description,                // doc.text.fill
        "folder" to Icons.Outlined.Folder,                          // folder.fill
        "inventory_2" to Icons.Outlined.Inventory2,                 // archivebox.fill
        "handyman" to Icons.Outlined.Handyman,                      // hammer.fill
        "build" to Icons.Outlined.Build,                            // wrench.and.screwdriver.fill
        // Santé
        "medical_services" to Icons.Outlined.MedicalServices,       // cross.case.fill
        "medication" to Icons.Outlined.Medication,                  // pills.fill
        "monitor_heart" to Icons.Outlined.MonitorHeart,             // stethoscope
        "favorite" to Icons.Outlined.Favorite,                      // heart.fill
        "healing" to Icons.Outlined.Healing,                        // bandage.fill
        // Cadeaux & études
        "card_giftcard" to Icons.Outlined.CardGiftcard,             // gift.fill
        "school" to Icons.Outlined.School,                          // graduationcap.fill
        "menu_book" to Icons.AutoMirrored.Outlined.MenuBook,        // book.fill
        "library_books" to Icons.AutoMirrored.Outlined.LibraryBooks, // books.vertical.fill
        // Sport & loisirs
        "directions_run" to Icons.AutoMirrored.Outlined.DirectionsRun,   // figure.run
        "directions_walk" to Icons.AutoMirrored.Outlined.DirectionsWalk, // figure.walk
        "sports_esports" to Icons.Outlined.SportsEsports,           // gamecontroller.fill
        "movie" to Icons.Outlined.Movie,                            // film.fill
        "music_note" to Icons.Outlined.MusicNote,                   // music.note
        "tv" to Icons.Outlined.Tv,                                  // tv.fill
        "photo_camera" to Icons.Outlined.PhotoCamera,               // camera.fill
        // Animaux & nature
        "pets" to Icons.Outlined.Pets,                              // pawprint.fill
        "cruelty_free" to Icons.Outlined.CrueltyFree,               // dog.fill
        "emoji_nature" to Icons.Outlined.EmojiNature,               // cat.fill
        "auto_awesome" to Icons.Outlined.AutoAwesome,               // sparkles
        "eco" to Icons.Outlined.Eco,                                // leaf.fill
        "park" to Icons.Outlined.Park,                              // tree.fill
        "local_florist" to Icons.Outlined.LocalFlorist,             // carrot.fill
        "set_meal" to Icons.Outlined.SetMeal,                       // fish.fill
        // Temps
        "schedule" to Icons.Outlined.Schedule,                      // clock.fill
        "calendar_month" to Icons.Outlined.CalendarMonth,           // calendar
        "alarm" to Icons.Outlined.Alarm,                            // alarm.fill
        "timer" to Icons.Outlined.Timer,                            // timer
        // Sécurité & lieux
        "shield" to Icons.Outlined.Shield,                          // shield.fill
        "lock" to Icons.Outlined.Lock,                              // lock.fill
        "place" to Icons.Outlined.Place,                            // mappin.and.ellipse
        "my_location" to Icons.Outlined.MyLocation,                 // location.fill
        "luggage" to Icons.Outlined.Luggage,                        // suitcase.fill
        "shopping_bag" to Icons.Outlined.ShoppingBag,               // bag.fill
    )

    /** Icône d'un symbole stocké en base ; retombe sur le symbole par défaut. */
    fun iconFor(symbol: String): ImageVector =
        all[symbol] ?: all.getValue(CustomCategory.DEFAULT_SYMBOL)
}
