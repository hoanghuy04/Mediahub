package com.bondhub.authservice.service.account;

import com.bondhub.authservice.dto.account.request.AccountCreateRequest;
import com.bondhub.authservice.dto.account.response.AccountResponse;
import com.bondhub.authservice.dto.account.request.AccountUpdateRequest;
import com.bondhub.common.dto.ApiResponse;

import java.util.List;

/**
 * Service interface for managing Account operations.
 * <p>
 * This service provides CRUD operations for Account entities using DTOs
 * and additional utility methods for checking account existence by email and phone number.
 * All methods return {@link ApiResponse} objects for consistent response handling.
 * </p>
 *
 * @author BondHub Development Team
 * @version 1.0
 * @since 2026-01-15
 */
public interface AccountService {

    /**
     * Creates a new account in the system.
     * <p>
     * Validates that the email and phone number are unique before creating the account.
     * </p>
     *
     * @param request the account creation request DTO, must not be null
     * @return {@link ApiResponse} containing the created account response DTO with generated ID
     *         <ul>
     *           <li>Success (code 1000): Account created successfully</li>
     *           <li>Error (code 1001): Email already exists</li>
     *           <li>Error (code 1002): Phone number already exists</li>
     *           <li>Error (code 5000): Server error during creation</li>
     *         </ul>
     */
    ApiResponse<AccountResponse> createAccount(AccountCreateRequest request);

    /**
     * Retrieves an account by its unique identifier.
     *
     * @param id the unique identifier of the account, must not be null
     * @return {@link ApiResponse} containing the account response DTO if found
     *         <ul>
     *           <li>Success (code 1000): Account found</li>
     *           <li>Error (code 1003): Account not found</li>
     *           <li>Error (code 5000): Server error during retrieval</li>
     *         </ul>
     */
    ApiResponse<AccountResponse> getAccountById(String id);

    /**
     * Retrieves an account by email address.
     *
     * @param email the email address to search for, must not be null
     * @return {@link ApiResponse} containing the account response DTO if found
     *         <ul>
     *           <li>Success (code 1000): Account found</li>
     *           <li>Error (code 1003): Account not found</li>
     *           <li>Error (code 5000): Server error during retrieval</li>
     *         </ul>
     */
    ApiResponse<AccountResponse> getAccountByEmail(String email);

    /**
     * Retrieves an account by phone number.
     *
     * @param phoneNumber the phone number to search for, must not be null
     * @return {@link ApiResponse} containing the account response DTO if found
     *         <ul>
     *           <li>Success (code 1000): Account found</li>
     *           <li>Error (code 1003): Account not found</li>
     *           <li>Error (code 5000): Server error during retrieval</li>
     *         </ul>
     */
    ApiResponse<AccountResponse> getAccountByPhoneNumber(String phoneNumber);

    /**
     * Retrieves all accounts in the system.
     *
     * @return {@link ApiResponse} containing a list of all account response DTOs
     *         <ul>
     *           <li>Success (code 1000): List of accounts (may be empty)</li>
     *           <li>Error (code 5000): Server error during retrieval</li>
     *         </ul>
     */
    ApiResponse<List<AccountResponse>> getAllAccounts();

    /**
     * Updates an existing account with new information.
     * <p>
     * Only the provided fields will be updated. If email or phone number is changed,
     * validates that the new value is unique before updating.
     * </p>
     *
     * @param id the unique identifier of the account to update, must not be null
     * @param request the account update request DTO containing updated information
     * @return {@link ApiResponse} containing the updated account response DTO
     *         <ul>
     *           <li>Success (code 1000): Account updated successfully</li>
     *           <li>Error (code 1001): New email already exists for another account</li>
     *           <li>Error (code 1002): New phone number already exists for another account</li>
     *           <li>Error (code 1003): Account not found</li>
     *           <li>Error (code 5000): Server error during update</li>
     *         </ul>
     */
    ApiResponse<AccountResponse> updateAccount(String id, AccountUpdateRequest request);

    /**
     * Deletes an account from the system.
     *
     * @param id the unique identifier of the account to delete, must not be null
     * @return {@link ApiResponse} with no data
     *         <ul>
     *           <li>Success (code 1000): Account deleted successfully</li>
     *           <li>Error (code 1003): Account not found</li>
     *           <li>Error (code 5000): Server error during deletion</li>
     *         </ul>
     */
    ApiResponse<Void> deleteAccount(String id);

    /**
     * Checks if an account with the specified email exists.
     *
     * @param email the email address to check, must not be null
     * @return {@link ApiResponse} containing a boolean value
     *         <ul>
     *           <li>Success (code 1000): true if account exists, false otherwise</li>
     *           <li>Error (code 5000): Server error during check</li>
     *         </ul>
     */
    ApiResponse<Boolean> existsByEmail(String email);

    /**
     * Checks if an account with the specified phone number exists.
     *
     * @param phoneNumber the phone number to check, must not be null
     * @return {@link ApiResponse} containing a boolean value
     *         <ul>
     *           <li>Success (code 1000): true if account exists, false otherwise</li>
     *           <li>Error (code 5000): Server error during check</li>
     *         </ul>
     */
    ApiResponse<Boolean> existsByPhoneNumber(String phoneNumber);
}

