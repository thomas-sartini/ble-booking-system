#if DEBUG
import Foundation
import SharedLogic

/// In-memory API for Xcode previews. Never contacts the backend.
final class PreviewMobileCoreApi: NSObject, MobileCoreApi {
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
        completionHandler([], nil)
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

    private var unsupportedOperation: NSError {
        NSError(domain: "PreviewMobileCoreApi", code: 1)
    }
}
#endif
