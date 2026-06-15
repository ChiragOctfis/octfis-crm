// app/src/main/java/com/octfis/crm/data/remote/ZohoApiService.kt
package com.octfis.crm.data.remote

import com.octfis.crm.data.remote.dto.*
import retrofit2.http.*

interface ZohoApiService {

    // ── Accounts ──────────────────────────────────────────────────────────────

    @GET("server/crmBridge")
    suspend fun getAccounts(
        @Query("page")        page      : Int    = 1,
        @Query("per_page")    perPage   : Int    = 100,
        @Query("module")      module    : String = "accounts",
        @Query("action")      action    : String = "list",
        @Query("sort_by")     sortBy    : String = "Modified_Time",
        @Query("sort_order")  sortOrder : String = "desc",
    ): AccountsResponse

    @GET("server/crmBridge")
    suspend fun getAccountById(
        @Query("id")     id     : String,
        @Query("module") module : String = "accounts",
        @Query("action") action : String = "get",

    ): AccountsResponse

    @GET("server/crmBridge")
    suspend fun getAccountContacts(
        @Query("account_id") accountId : String,
        @Query("module")     module    : String = "contacts",
        @Query("action")     action    : String = "list",
        @Query("per_page")   perPage   : Int    = 200,
    ): ContactsResponse

    @POST("server/crmBridge")
    suspend fun createAccount(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "accounts",
        @Query("action") action : String = "create",
    ): CreateRecordResponse

    @PUT("server/crmBridge")
    suspend fun updateAccount(
        @Query("id")     id     : String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "accounts",
        @Query("action") action : String = "update",
    ): CreateRecordResponse

    // ── Contacts ──────────────────────────────────────────────────────────────

    @GET("server/crmBridge")
    suspend fun getContacts(
        @Query("page")       page      : Int    = 1,
        @Query("per_page")   perPage   : Int    = 100,
        @Query("module")     module    : String = "contacts",
        @Query("action")     action    : String = "list",
        @Query("sort_by")    sortBy    : String = "Modified_Time",
        @Query("sort_order") sortOrder : String = "desc",
        @Query("fields")     fields    : String = "id,First_Name,Last_Name,Full_Name,Phone,Mobile,Email,Account_Name,Title,Department,Lead_Source,Contact_Owner,Description,Mailing_Street,Mailing_City,Mailing_State,Mailing_Zip,Mailing_Country",
    ): ContactsResponse

    @POST("server/crmBridge")
    suspend fun createContact(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "contacts",
        @Query("action") action : String = "create",
    ): CreateRecordResponse

    @PUT("server/crmBridge")
    suspend fun updateContact(
        @Query("id")     id     : String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "contacts",
        @Query("action") action : String = "update",
    ): CreateRecordResponse

    // ── Deals ─────────────────────────────────────────────────────────────────

    @GET("server/crmBridge")
    suspend fun getDeals(
        @Query("page")       page      : Int    = 1,
        @Query("per_page")   perPage   : Int    = 100,
        @Query("module")     module    : String = "deals",
        @Query("action")     action    : String = "list",
        @Query("sort_by")    sortBy    : String = "Modified_Time",
        @Query("sort_order") sortOrder : String = "desc",
    ): DealsResponse

    @GET("server/crmBridge")
    suspend fun getDealById(
        @Query("id")     id     : String,
        @Query("module") module : String = "deals",
        @Query("action") action : String = "get",
    ): DealsResponse

    @POST("server/crmBridge")
    suspend fun createDeal(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "deals",
        @Query("action") action : String = "create",

    ): CreateRecordResponse

    @PUT("server/crmBridge")
    suspend fun updateDeal(
        @Query("id")     id     : String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "deals",
        @Query("action") action : String = "update",
    ): CreateRecordResponse

    // ── Quotes ────────────────────────────────────────────────────────────────

    @GET("server/crmBridge")
    suspend fun getQuotes(
        @Query("page")       page      : Int    = 1,
        @Query("per_page")   perPage   : Int    = 100,
        @Query("module")     module    : String = "quotes",
        @Query("action")     action    : String = "list",
        @Query("sort_by")    sortBy    : String = "Modified_Time",
        @Query("sort_order") sortOrder : String = "desc",
    ): QuotesResponse

    @GET("server/crmBridge")
    suspend fun getQuoteById(
        @Query("id")     id     : String,
        @Query("module") module : String = "quotes",
        @Query("action") action : String = "get",
    ): QuotesResponse

    @POST("server/crmBridge")
    suspend fun createQuote(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "quotes",
        @Query("action") action : String = "create",
    ): CreateRecordResponse

    @PUT("server/crmBridge")
    suspend fun updateQuote(
        @Query("id")     id     : String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "quotes",
        @Query("action") action : String = "update",
    ): CreateRecordResponse

    // ── Products ──────────────────────────────────────────────────────────────

    @GET("server/crmBridge")
    suspend fun getProducts(
        @Query("page")       page      : Int    = 1,
        @Query("per_page")   perPage   : Int    = 200,
        @Query("module")     module    : String = "products",
        @Query("action")     action    : String = "list",
        @Query("sort_by")    sortBy    : String = "Product_Name",
        @Query("sort_order") sortOrder : String = "asc",
        @Query("fields")     fields    : String = "Product_Name,Unit_Price,Product_Code",
    ): ProductsResponse

    // ── Tasks ─────────────────────────────────────────────────────────────────

    @GET("server/crmBridge")
    suspend fun getTasks(
        @Query("sort_by")    sortBy    : String = "Due_Date",
        @Query("sort_order") sortOrder : String = "asc",
        @Query("module")     module    : String = "tasks",
        @Query("action")     action    : String = "list",
        @Query("page")       page      : Int    = 1,
        @Query("per_page")   perPage   : Int    = 50,
    ): TasksResponse

    @GET("server/crmBridge")
    suspend fun getTaskById(
        @Query("id")     id     : String,
        @Query("module") module : String = "tasks",
        @Query("action") action : String = "get",
    ): TasksResponse

    @POST("server/crmBridge")
    suspend fun createTask(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "tasks",
        @Query("action") action : String = "create",

    ): CreateRecordResponse

    @PUT("server/crmBridge")
    suspend fun updateTask(
        @Query("id")     id     : String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "tasks",
        @Query("action") action : String = "update",
    ): CreateRecordResponse

    @DELETE("server/crmBridge")
    suspend fun deleteTask(
        @Query("id")     id     : String,
        @Query("module") module : String = "tasks",
        @Query("action") action : String = "delete",

    ): CreateRecordResponse

    // ── Events / Meetings ─────────────────────────────────────────────────────

    @GET("server/crmBridge")
    suspend fun getEvents(
        @Query("module")     module    : String = "events",
        @Query("action")     action    : String = "list",
        @Query("page")       page      : Int    = 1,
        @Query("per_page")   perPage   : Int    = 50,
        @Query("sort_by")    sortBy    : String = "Start_DateTime",
        @Query("sort_order") sortOrder : String = "desc",
    ): EventsResponse

    @GET("server/crmBridge")
    suspend fun getEventById(
        @Query("id")     id     : String,
        @Query("module") module : String = "events",
        @Query("action") action : String = "get",
    ): EventsResponse

    @POST("server/crmBridge")
    suspend fun createEvent(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "events",
        @Query("action") action : String = "create",
    ): CreateRecordResponse

    @PUT("server/crmBridge")
    suspend fun updateEvent(
        @Query("id")     id     : String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "events",
        @Query("action") action : String = "update",
    ): CreateRecordResponse

    @DELETE("server/crmBridge")
    suspend fun deleteEvent(
        @Query("id")     id     : String,
        @Query("module") module : String = "events",
        @Query("action") action : String = "delete",
    ): CreateRecordResponse

    // ── Calls ─────────────────────────────────────────────────────────────────

    @GET("server/crmBridge")
    suspend fun getCalls(
        @Query("module")     module    : String = "calls",
        @Query("action")     action    : String = "list",
        @Query("page")       page      : Int =1,
        @Query("per_page")   perPage   : Int    = 50,
        @Query("sort_by")    sortBy    : String = "Call_Start_Time",
        @Query("sort_order") sortOrder : String = "desc",
    ): CallsResponse

    @GET("server/crmBridge")
    suspend fun getCallById(
        @Query("id")     id     : String,
        @Query("module") module : String = "calls",
        @Query("action") action : String = "get",
    ): CallsResponse

    @GET("server/crmBridge")
    suspend fun getCallsForContact(
        @Query("contact_id") contactId : String,
        @Query("module")     module    : String = "calls",
        @Query("action")     action    : String = "list",
        @Query("page")       page      : Int,
        @Query("per_page")   perPage   : Int    = 50,
    ): CallsResponse

    @POST("server/crmBridge")
    suspend fun createCall(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "calls",
        @Query("action") action : String = "create",
    ): CreateRecordResponse

    @PUT("server/crmBridge")
    suspend fun updateCall(
        @Query("id")     id     : String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("module") module : String = "calls",
        @Query("action") action : String = "update",
    ): CreateRecordResponse

    @DELETE("server/crmBridge")
    suspend fun deleteCall(
        @Query("id")     id     : String,
        @Query("module") module : String = "calls",
        @Query("action") action : String = "delete",
    ): CreateRecordResponse

    // ── Settings ──────────────────────────────────────────────────────────────

    @GET("server/crmBridge/fields")
    suspend fun getFields(@Query("module") module: String): FieldsResponse

    @GET("server/crmBridge/users")
    suspend fun getUsers(@Query("type") type: String = "AllUsers"): UsersResponse
}