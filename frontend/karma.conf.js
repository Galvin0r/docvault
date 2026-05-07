module.exports = (config) => {
  config.set({
    browsers: ['ChromeHeadless'],
    frameworks: ["jasmine", "@angular-devkit/build-angular"],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    reporters: ["progress"],
  });
};