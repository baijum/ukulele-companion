import SwiftUI

private struct LicenseEntry: Identifiable {
    let id: String
    let name: String
    let copyright: String
    let licenseType: String
    let url: String
    let licenseText: String
}

private let licenseEntries: [LicenseEntry] = [
    LicenseEntry(
        id: "onnxruntime",
        name: "ONNX Runtime",
        copyright: "Copyright (c) Microsoft Corporation",
        licenseType: "MIT License",
        url: "https://github.com/microsoft/onnxruntime",
        licenseText: """
            MIT License

            Copyright (c) Microsoft Corporation

            Permission is hereby granted, free of charge, to any person obtaining a copy \
            of this software and associated documentation files (the "Software"), to deal \
            in the Software without restriction, including without limitation the rights \
            to use, copy, modify, merge, publish, distribute, sublicense, and/or sell \
            copies of the Software, and to permit persons to whom the Software is \
            furnished to do so, subject to the following conditions:

            The above copyright notice and this permission notice shall be included in all \
            copies or substantial portions of the Software.

            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR \
            IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, \
            FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE \
            AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER \
            LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, \
            OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE \
            SOFTWARE.
            """
    ),
    LicenseEntry(
        id: "swiftf0",
        name: "SwiftF0",
        copyright: "Copyright (c) lars76",
        licenseType: "MIT License",
        url: "https://github.com/lars76/swift-f0",
        licenseText: """
            MIT License

            Copyright (c) lars76

            Permission is hereby granted, free of charge, to any person obtaining a copy \
            of this software and associated documentation files (the "Software"), to deal \
            in the Software without restriction, including without limitation the rights \
            to use, copy, modify, merge, publish, distribute, sublicense, and/or sell \
            copies of the Software, and to permit persons to whom the Software is \
            furnished to do so, subject to the following conditions:

            The above copyright notice and this permission notice shall be included in all \
            copies or substantial portions of the Software.

            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR \
            IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, \
            FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE \
            AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER \
            LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, \
            OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE \
            SOFTWARE.
            """
    ),
]

struct OpenSourceLicensesView: View {
    @State private var expandedEntry: String?

    var body: some View {
        List(licenseEntries) { entry in
            VStack(alignment: .leading, spacing: 8) {
                Button {
                    withAnimation {
                        expandedEntry = expandedEntry == entry.id ? nil : entry.id
                    }
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(entry.name)
                                .font(.body.bold())
                                .foregroundColor(.primary)
                            Text(entry.licenseType)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Image(systemName: expandedEntry == entry.id ? "chevron.up" : "chevron.down")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .accessibilityHidden(true)
                    }
                }
                .accessibilityHint(expandedEntry == entry.id
                    ? String(localized: "Double-tap to collapse")
                    : String(localized: "Double-tap to expand"))

                if expandedEntry == entry.id {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(entry.copyright)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        if let url = URL(string: entry.url) {
                            Link(destination: url) {
                                Text(entry.url)
                                    .font(.caption)
                            }
                            .accessibilityHint(String(localized: "Opens in Safari"))
                        }
                        Text(entry.licenseText)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    .transition(.opacity)
                }
            }
        }
        .navigationTitle("Open Source Licenses")
        .navigationBarTitleDisplayMode(.inline)
    }
}
