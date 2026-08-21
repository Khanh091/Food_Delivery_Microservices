import { registerRootComponent } from "expo";

// Background location tasks must be defined at module scope before React mounts.
import "./src/features/location/services/backgroundLocationTask";

import App from "./App";

// registerRootComponent calls AppRegistry.registerComponent('main', () => App);
// It also ensures that whether you load the app in Expo Go or in a native build,
// the environment is set up appropriately
registerRootComponent(App);
