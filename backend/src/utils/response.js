/**
 * Standard Response Helpers
 * 
 * These functions provide consistent response formats for the API
 */

/**
 * Send a success response
 * @param {Response} res - Express response object
 * @param {any} data - Data to send
 * @param {string} message - Optional success message
 * @param {number} statusCode - HTTP status code (default: 200)
 */
const success = (res, data, message = 'Success', statusCode = 200) => {
    return res.status(statusCode).json({
        success: true,
        message,
        data
    });
};

/**
 * Send an error response
 * @param {Response} res - Express response object
 * @param {string} message - Error message
 * @param {number} statusCode - HTTP status code (default: 400)
 * @param {any} details - Optional error details
 */
const error = (res, message = 'Something went wrong', statusCode = 400, details = null) => {
    const response = {
        success: false,
        message
    };

    if (details) {
        response.details = details;
    }

    return res.status(statusCode).json(response);
};

/**
 * Send a not found response
 */
const notFound = (res, message = 'Resource not found') => {
    return error(res, message, 404);
};

/**
 * Send an unauthorized response
 */
const unauthorized = (res, message = 'Unauthorized') => {
    return error(res, message, 401);
};

/**
 * Send a forbidden response
 */
const forbidden = (res, message = 'Forbidden') => {
    return error(res, message, 403);
};

module.exports = {
    success,
    error,
    notFound,
    unauthorized,
    forbidden
};
