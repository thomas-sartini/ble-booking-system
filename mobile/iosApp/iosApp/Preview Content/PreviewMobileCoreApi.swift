#if DEBUG
import Foundation
import SharedLogic

/// In-memory API for Xcode previews. Never contacts the backend.
final class PreviewMobileCoreApi: NSObject, MobileCoreApi {
    func restoreSession(completionHandler: @escaping (SessionRestoreResult?, Error?) -> Void) {
        completionHandler(SessionRestoreResultUnauthenticated.shared, nil)
    }

    func login(
        identifier: String,
        password: String,
        completionHandler: @escaping (Error?) -> Void
    ) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            completionHandler(nil)
        }
    }

    func logout(completionHandler: @escaping (Error?) -> Void) {
        completionHandler(nil)
    }

    func getActiveBookings(
        completionHandler: @escaping ([Booking]?, Error?) -> Void
    ) {
        completionHandler([
            Booking(
                id: "1",
                resourceId: "101",
                startTime: .companion.parse(input: "2026-11-25T09:00:00Z"),
                endTime: .companion.parse(input: "2026-11-25T10:00:00Z"),
                status: .checkedIn,
                resourceName: "Meeting Room 101",
                resourcePosition: "First floor · East wing"
            ),
            Booking(
                id: "2",
                resourceId: "204",
                startTime: .companion.parse(input: "2026-11-26T13:00:00Z"),
                endTime: .companion.parse(input: "2026-11-26T14:30:00Z"),
                status: .booked,
                resourceName: "Media Lab",
                resourcePosition: "Second floor"
            )
        ], nil)
    }

    func requestAccess(
        request: ResourceAccessRequest,
        completionHandler: @escaping (ResourceAccessResponse?, Error?) -> Void
    ) {
        completionHandler(nil, unsupportedOperation)
    }

    func confirmAccess(
        confirmation: AccessConfirmation,
        completionHandler: @escaping (Error?) -> Void
    ) {
        completionHandler(unsupportedOperation)
    }

    func checkOut(
        resourceId: String,
        completionHandler: @escaping (Error?) -> Void
    ) {
        completionHandler(unsupportedOperation)
    }

    func getUserProfile(
        completionHandler: @escaping (UserProfile?, Error?) -> Void
    ) {
        completionHandler(
            UserProfile(
                firstName: "Alex",
                lastName: "Morgan",
                username: "amorgan",
                email: "alex@company.com",
                role: .student
            ),
            nil
        )
    }

    private var unsupportedOperation: NSError {
        NSError(domain: "PreviewMobileCoreApi", code: 1)
    }
}
#endif
