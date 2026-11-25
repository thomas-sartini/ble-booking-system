import SwiftUI

@MainActor
struct LoginView: View {
    @Bindable var viewModel: LoginViewModel
    @State private var isPasswordVisible = false
    @FocusState private var focusedField: Field?

    let messageData: MessageData?

    private enum Field: Hashable {
        case email
        case password
    }

    init(
        viewModel: LoginViewModel,
        messageData: MessageData? = nil
    ) {
        self.viewModel = viewModel
        self.messageData = messageData
    }

    var body: some View {
        Form {
            Section {
                header
            }
            .listRowBackground(Color.clear)

            if viewModel.errorMessage == nil, let messageData {
                messageSection(text: messageData.text, type: messageData.type)
            }

            Section {
                emailField
                passwordField
            } footer: {
                forgotPasswordButton
            }
            .listRowBackground(BLETheme.surface)
            .disabled(viewModel.isLoading)

            if let errorMessage = viewModel.errorMessage {
                messageSection(text: errorMessage, type: .error)
            }

            Section {
                signInButton
            }
            .listRowInsets(EdgeInsets())
            .listRowBackground(Color.clear)
        }
        .formStyle(.grouped)
        .scrollContentBackground(.hidden)
        .background(BLETheme.background)
        .scrollDismissesKeyboard(.interactively)
        .onDisappear {
            viewModel.cancelSignIn()
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("login_brand", systemImage: "key.fill")
                .font(.headline)
                .foregroundStyle(BLETheme.accent)

            Text("login_title")
                .font(.largeTitle.bold())
                .foregroundStyle(BLETheme.text)
                .accessibilityAddTraits(.isHeader)

            Text("login_subtitle")
                .font(.body)
                .foregroundStyle(BLETheme.secondaryText)
        }
        .padding(.vertical, 16)
    }

    private var emailField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("login_email")
                .font(.subheadline)
                .foregroundStyle(BLETheme.secondaryText)

            TextField("login_email_hint", text: $viewModel.email)
                .keyboardType(.emailAddress)
                .textContentType(.username)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.next)
                .focused($focusedField, equals: .email)
                .accessibilityLabel(Text("login_email"))
                .onSubmit { focusedField = .password }
        }
        .padding(.vertical, 4)
    }

    private var passwordField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("login_password")
                .font(.subheadline)
                .foregroundStyle(BLETheme.secondaryText)

            HStack(spacing: 12) {
                Group {
                    if isPasswordVisible {
                        TextField("login_password_hint", text: $viewModel.password)
                    } else {
                        SecureField("login_password_hint", text: $viewModel.password)
                    }
                }
                .textContentType(.password)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.go)
                .focused($focusedField, equals: .password)
                .accessibilityLabel(Text("login_password"))
                .onSubmit(submitSignIn)

                Button {
                    let wasFocused = focusedField == .password
                    isPasswordVisible.toggle()
                    if wasFocused {
                        focusedField = .password
                    }
                } label: {
                    Image(systemName: isPasswordVisible ? "eye.slash" : "eye")
                        .frame(minWidth: 44, minHeight: 44)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(
                    isPasswordVisible ? Text("login_hide") : Text("login_show")
                )
            }
        }
        .padding(.vertical, 4)
    }

    private var forgotPasswordButton: some View {
        HStack {
            Spacer()
            Button("login_forgot") {
                onForgotPassword()
            }
            .disabled(viewModel.isLoading)
        }
        .padding(.top, 8)
    }

    private var signInButton: some View {
        Button(action: submitSignIn) {
            HStack(spacing: 12) {
                if viewModel.isLoading {
                    ProgressView()
                        .tint(BLETheme.onAccent)
                }

                Text("login_sign_in")
                    .fontWeight(.semibold)
            }
        }
        .buttonStyle(BLEPrimaryButtonStyle())
        .disabled(!viewModel.canSignIn)
    }

    private func messageSection(text: String, type: MessageType) -> some View {
        Section {
            Message(text: text, type: type)
        }
        .listRowInsets(EdgeInsets())
        .listRowBackground(Color.clear)
    }

    private func submitSignIn() {
        focusedField = nil
        viewModel.signIn()
    }

    private func onForgotPassword() {
        // Integrata internamente come nella versione Android
    }
}
